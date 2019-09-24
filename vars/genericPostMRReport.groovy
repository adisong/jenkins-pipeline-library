import com.adisong.jenkins.helpers.GitHelper
import com.adisong.jenkins.helpers.SonarqubeHelper

def call(def params = [:]) {
    def repoCredentials
    def gitlabConnection
    def buildFlavorConfig
    def gitHelper, sonarqubeHelper
    def sonarInstallationName

    // Setup
    // withFolderProperties requires jenkins agent context to work so we need to execute it inside node block
    node('master') {
        withFolderProperties {
            repoCredentials = "${env.gitCredentialsId}"
            gitlabConnection = "${env.gitlabConnection}"

            def buildFlavorConfigDef = libraryResource("com/adisong/jenkins/pipeline-library/config/build/flavor/${params.buildFlavor}.json")
            buildFlavorConfig = jsonParse("${buildFlavorConfigDef}")

            gitHelper = new GitHelper(this, "${env.gitlabSourceRepoHttpUrl}", "${repoCredentials}")

            sonarInstallationName = "${env.sonarInstallationName}"
            sonarqubeHelper = new SonarqubeHelper(this,sonarInstallationName)
        }
    }

    pipeline {
        agent { label "${params.agentLabel}" }
        options {
            gitLabConnection("${gitlabConnection}")
            gitlabBuilds(builds: ['checkout','staticCodeAnalysis'])
        }
        environment {
            JAVA_HOME = "${env.BUILD_JAVA_HOME != null ? env.BUILD_JAVA_HOME : env.JAVA_HOME}"
        }
        stages {
            stage ('checkout'){
                steps {
                    script {
                        gitHelper.CheckoutWithSubmodules("${env.gitlabTargetBranch}")

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
            stage('Perform post MR tasks') {
                failFast false
                parallel {
                    stage('staticCodeAnalysis') {
                        steps {
                            script {
                                String projectKeyBase = "${env.gitlabTargetNamespace}:${env.gitlabTargetRepoName}"
                                String name = "${env.gitlabTargetNamespace}:${env.gitlabTargetRepoName}:${env.gitlabTargetBranch}"
                                String branch = "${env.gitlabTargetBranch.replace('/', '_')}"
                                String projectKey = "${projectKeyBase}:${branch}"
                                try { //ignore error for now as it may be raised because project already exist
                                    sonarqubeHelper.CreateProject(projectKeyBase, name, branch)
                                } catch (err) {
                                }

                                String language = "${params.get('sonarLanguage', buildFlavorConfig.language)}"
                                String qualityProfile = "${params.get('sonarQualityProfile', buildFlavorConfig.defaultQualityProfile)}"
                                sonarqubeHelper.SetQualityProfile("${projectKey}", language, qualityProfile)

                                if (buildFlavorConfig.steps.analyze.prerequisites != null) {
                                    buildFlavorConfig.steps.analyze.prerequisites.each { target ->
                                        sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${target}"
                                    }
                                }
                                withSonarQubeEnv("${sonarInstallationName}") {
                                    sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${buildFlavorConfig.steps.analyze.target} -Dsonar.projectKey=${projectKey} -Dsonar.projectName=${name}"
                                }
                            }
                        }
                        post {
                            always {
                                script {
                                    String projectKeyBase = "${env.gitlabSourceNamespace}:${env.gitlabSourceRepoName}"
                                    String branch = "${env.gitlabSourceBranch.replace('/', '_')}"
                                    String projectKey = "${projectKeyBase}:${branch}"
                                    sonarqubeHelper.DeleteProject(projectKey)
                                }
                            }
                            failure {
                                updateGitlabCommitStatus name: 'staticCodeAnalysis', state: 'failed'
                            }
                            success {
                                updateGitlabCommitStatus name: 'staticCodeAnalysis', state: 'success'
                            }
                        }
                    }
                    stage('bump RC tag') {
                        when {
                            expression {
                                "${env.gitlabTargetBranch}" ==~ /^(release|hotfix)\/(\d+\.\d+\.\d+)$/
                            }
                        }
                        steps {
                            script {
                                sh "git checkout ${env.gitlabTargetBranch}"
                                gitHelper.publishNextRCTag()
                            }
                        }
                    }
                }
            }
        }
    }
}
