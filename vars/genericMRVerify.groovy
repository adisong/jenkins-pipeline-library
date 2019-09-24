import com.adisong.jenkins.helpers.GitHelper
import com.adisong.jenkins.helpers.SonarqubeHelper
import com.adisong.jenkins.helpers.NexusIQHelper

def call(def params = [:]) {
    def repoCredentials
    def gitlabConnection
    def buildFlavorConfig
    def gitHelper, sonarqubeHelper, nexusIQHelper
    def nexusIQAppName, nexusIQScanPatterns, sonarInstallationName

    // Setup
    // withFolderProperties requires jenkins agent context to work so we need to execute it inside node block
    node('master') {
        withFolderProperties {
            repoCredentials = "${env.gitCredentialsId}"
            gitlabConnection = "${env.gitlabConnection}"

            def buildFlavorConfigDef = libraryResource("com/adisong/jenkins/pipeline-library/config/build/flavor/${params.buildFlavor}.json")
            buildFlavorConfig = jsonParse("${buildFlavorConfigDef}")

            gitHelper = new GitHelper(this, "${env.gitlabSourceRepoHttpUrl}", "${repoCredentials}")
            nexusIQHelper = new NexusIQHelper()

            sonarInstallationName = "${env.sonarInstallationName}"
            sonarqubeHelper = new SonarqubeHelper(this,sonarInstallationName)

            nexusIQAppName = "${params.get('nexusIQAppName', "${env.gitlabSourceRepoName}")}"
            nexusIQScanPatterns = params.get('nexusIQScanPatterns', buildFlavorConfig.steps.securityScan.scanPatterns)
        }
    }

    pipeline {
        agent { label "${params.agentLabel}" }
        options {
            gitLabConnection("${gitlabConnection}")
            gitlabBuilds(builds: ['checkout','syntaxCheck', 'test', 'staticCodeAnalysis', 'securityEvaluation'])
        }
        environment {
            JAVA_HOME = "${env.BUILD_JAVA_HOME != null ? env.BUILD_JAVA_HOME : env.JAVA_HOME}"
        }
        stages {
            stage ('checkout'){
                steps {
                    script {
                        gitHelper.CheckoutWithPreBuildMerge("${env.gitlabSourceBranch}","${env.gitlabTargetBranch}")

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
            stage('syntaxCheck'){
                steps {
                    sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${buildFlavorConfig.steps.syntaxCheck.target}"
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'syntaxCheck', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'syntaxCheck', state: 'success'
                    }
                }
            }
            stage('test'){
                steps {
                    sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${buildFlavorConfig.steps.test.target}"
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'test', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'test', state: 'success'
                    }
                }
            }
            stage('staticCodeAnalysis'){
                steps {
                    script {
                        String projectKeyBase = "${env.gitlabSourceNamespace}:${env.gitlabSourceRepoName}"
                        String name = "${env.gitlabSourceNamespace}:${env.gitlabSourceRepoName}:${env.gitlabSourceBranch}"
                        String branch = "${env.gitlabSourceBranch.replace('/','_')}" //for branches with '/' as this character is not allowed as projectKey
                        String projectKey = "${projectKeyBase}:${branch}"
                        try { //ignore error for now as it may be raised because project already exist
                            sonarqubeHelper.CreateProject(projectKeyBase, name, branch)
                        } catch (err) {}

                        String language = "${params.get('sonarLanguage',buildFlavorConfig.language)}"
                        String qualityProfile = "${params.get('sonarQualityProfile',buildFlavorConfig.defaultQualityProfile)}"
                        sonarqubeHelper.SetQualityProfile(projectKey, language, qualityProfile)

                        env.gitlabMergeRequestCommitsDiff = gitHelper.getMergeRequestCommitsDiff("origin/${env.gitlabSourceBranch}", "origin/${env.gitlabTargetBranch}")
                        withSonarQubeEnv("${sonarInstallationName}") {
                            sh "${buildFlavorConfig.buildTool} --stacktrace ${buildFlavorConfig.debugFlags} ${buildFlavorConfig.steps.analyze.target} ${SONAR_GITLAB_PARAMS} -Dsonar.projectKey=${projectKey} -Dsonar.projectName=${name}"
                        }
                    }
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'staticCodeAnalysis', state: 'failed'
                    }
                    success {
                        script {
                            def dashboardUrls = sh(label: '', returnStdout: true, script: 'cat $(find . -name report-task.txt) | grep -oP \'dashboardUrl=\\K\\S+\'')
                            addGitLabMRComment(comment: "Sonarqube dashboard URL: ${dashboardUrls}")
                        }
                        updateGitlabCommitStatus name: 'staticCodeAnalysis', state: 'success'
                    }
                }
            }
            stage('securityEvaluation'){
                steps {
                    script {
                        if (buildFlavorConfig.steps.securityScan.prerequisites != null) {
                            buildFlavorConfig.steps.securityScan.prerequisites.each { target ->
                                sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${target}"
                            }
                        }
                        def scanPatterns = []
                        nexusIQScanPatterns.each { pattern ->
                            scanPatterns.add([scanPattern: "${pattern}"])
                        }
                        def evaluationResult = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: selectedApplication("${nexusIQAppName}"), iqScanPatterns: scanPatterns, iqStage: 'build', jobCredentialsId: ''
                        addGitLabMRComment comment: "${nexusIQHelper.prepareMRComment(evaluationResult)}"
                    }
                }
                post {
                    failure {
                        updateGitlabCommitStatus name: 'securityEvaluation', state: 'failed'
                    }
                    success {
                        updateGitlabCommitStatus name: 'securityEvaluation', state: 'success'
                    }
                }
            }
        }
    }
}
