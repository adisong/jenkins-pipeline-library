import com.adisong.jenkins.helpers.GitHelper

def call(def params = [:]) {
    def dockerImageName, dockerImageTag, dockerfilePath
    def repoCredentials
    def gitlabConnection
    def buildFlavorConfig
    def gitHelper
    def dockerAgentLabel, deployAgentLabel
    def emailRecipient
    def lockResource

    // Setup
    // withFolderProperties requires jenkins agent context to work so we need to execute it inside node block
    node('master') {
        withFolderProperties {
            repoCredentials = "${env.gitCredentialsId}"
            gitlabConnection = "${env.gitlabConnection}"

            def buildFlavorConfigDef = libraryResource("com/adisong/jenkins/pipeline-library/config/build/flavor/${params.buildFlavor}.json")
            buildFlavorConfig = jsonParse("${buildFlavorConfigDef}")

            gitHelper = new GitHelper(this, "${env.gitlabSourceRepoHttpUrl}", "${repoCredentials}")

            dockerImageName = "${params.get('dockerImageName', "${env.gitlabSourceRepoName}")}"
            dockerfilePath = "${params.get('dockerfilePath', "Dockerfile")}"
            dockerBuildContext = "${params.get('dockerBuildContext','.')}"

            dockerAgentLabel = "${params.get('dockerAgentLabel', "${env.dockerAgentLabel}")}"
            deployAgentLabel = "${params.get('deployAgentLabel', "${env.deployAgentLabel}")}"
            lockResource = "${params.get('deployLockResource', "")}"

            if(env.gitlabUserEmail != null) {
                emailRecipient = "${env.gitlabUserEmail}"
            }
        }
    }

    pipeline {
        agent { label "${params.agentLabel}" }
        options {
            gitLabConnection("${gitlabConnection}")
            gitlabBuilds(builds: ['checkout','buildArtifacts', 'buildImage', 'deployDev'])
        }
        stages {
            stage ('checkout'){
                steps {
                    script {
                        gitHelper.CheckoutWithSubmodules("${env.gitlabSourceBranch}")
                        dockerImageTag = gitHelper.getShortSha()

                        if (buildFlavorConfig.buildProperties != null) {
                            buildFlavorConfig.buildProperties.each { buildProperty ->
                                writeFile file: "${buildProperty.target}", text: libraryResource("${buildProperty.name}")
                            }
                        }
                    }
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'checkout', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'checkout', state: 'success'
                    }
                }
            }
            stage('buildArtifacts'){
                environment {
                    JAVA_HOME = "${env.BUILD_JAVA_HOME != null ? env.BUILD_JAVA_HOME : env.JAVA_HOME}"
                }
                steps {
                    sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${buildFlavorConfig.steps.build.target}"
                    stash name: "artifacts", includes: "${params.artifactsInclude}"
                    stash name: "dockerfile", includes: "${dockerfilePath}"
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'buildArtifacts', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'buildArtifacts', state: 'success'
                    }
                }
            }
            stage('buildImage'){
                agent { label "${dockerAgentLabel}" }
                steps {
                    unstash "artifacts"
                    unstash "dockerfile"

                    withFolderProperties {
                        script {
                            docker.withRegistry("${env.baseImageRegistryProto}://${env.baseImageRegistry}", "${env.baseImageRegistryCredentialsId}") {
                                def dockerImage = docker.build("${env.destImageRegistryNamespace}/${dockerImageName}:${dockerImageTag}","-f ${dockerfilePath} ${dockerBuildContext}")
                                docker.withRegistry("${env.destImageRegistryProto}://${env.destImageRegistry}", "${env.destImageRegistryCredentialsId}") {
                                    dockerImage.push()
                                }
                            }
                        }
                    }
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'buildImage', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'buildImage', state: 'success'
                    }
                }
            }
            stage('deployDev'){
                agent { label "${deployAgentLabel}" }
                steps {
                    withFolderProperties {
                        script {
                            def deployParams = [
                                    credentialsId : "${env.k8sCredentialsId}",
                                    contextName   : 'dev-int',
                                    deploymentName: "${env.k8sDeploymentName}",
                                    containerName : "${env.k8sContainerName}",
                                    imageName     : "${env.destImageRegistry}/${env.destImageRegistryNamespace}/${dockerImageName}",
                                    imageTag      : "${dockerImageTag}",
                                    timeout       : 300
                            ]
                            boolean shouldLock = lockResource != ''
                            lockWhen([resource: "${lockResource}"], shouldLock) {
                                kubectlSetImage(deployParams)
                            }
                        }
                    }
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'deployDev', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'deployDev', state: 'success'
                    }
                }
            }
        }
        post {
            always {
                emailext body: "${currentBuild.currentResult}: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\n More info at: ${env.BUILD_URL}",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
                        to: "${emailRecipient}",
                        subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}"
            }
        }
    }
}
