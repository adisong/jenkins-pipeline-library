import com.adisong.jenkins.helpers.GitHelper

def call(def params = [:]) {
    def dockerImageName, dockerImageTag
    def repoCredentials
    def gitlabConnection
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

            gitHelper = new GitHelper(this, "${env.gitlabSourceRepoHttpUrl}", "${repoCredentials}")

            dockerImageName = "${params.get('dockerImageName', "${env.gitlabSourceRepoName}")}"

            dockerAgentLabel = "${params.get('dockerAgentLabel', "${env.dockerAgentLabel}")}"
            deployAgentLabel = "${params.get('deployAgentLabel', "${env.deployAgentLabel}")}"
            lockResource = "${params.get('deployLockResource', "")}"

            if(env.gitlabUserEmail != null) {
                emailRecipient = "${env.gitlabUserEmail}"
            }
        }
    }

    pipeline {
        agent { label "${dockerAgentLabel}" }
        options {
            gitLabConnection("${gitlabConnection}")
            gitlabBuilds(builds: ['promoteImage', 'tagMaster', 'deployProd'])
        }
        stages {
            stage('promoteImage'){
                steps {
                    withFolderProperties {
                        script {
                            docker.withRegistry("${env.destImageRegistryProto}://${env.destImageRegistry}", "${env.destImageRegistryCredentialsId}") {
                                dockerImageTag = gitHelper.getVersionFromBranch("${gitlabSourceBranch}")
                                def latestRCTag = gitHelper.getLatestRCTag("${dockerImageTag}")
                                def latestImageName = "${env.destImageRegistryNamespace}/${dockerImageName}:${latestRCTag}"
                                def dockerImage = docker.image("${latestImageName}")
                                dockerImage.pull()
                                // following sh is required because Image.push() is expecting to have image without registry to retag properly
                                sh "docker tag ${dockerImage.imageName()} ${latestImageName}"
                                dockerImage.push("${dockerImageTag}")
                            }
                        }
                    }
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'promoteImage', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'promoteImage', state: 'success'
                    }
                }
            }
            stage('tagMaster'){
                steps {
                    script {
                        sh "git checkout master"
                        sh "git tag ${dockerImageTag}"
                        gitHelper.pushRev("${dockerImageTag}")
                    }
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'tagMaster', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'tagMaster', state: 'success'
                    }
                }
            }
            stage('deployProd'){
                agent { label "${deployAgentLabel}" }
                steps {
                    withFolderProperties {
                        script {
                            def deployParams = [
                                    credentialsId : "${env.k8sCredentialsId}",
                                    contextName   : 'prod',
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
                        updateGitlabCommitStatus name: 'deployProd', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'deployProd', state: 'success'
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
