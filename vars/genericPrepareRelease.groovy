import com.adisong.jenkins.helpers.GitHelper

def call(def params = [:]) {
    def buildFlavorConfig
    def gitHelper   

    // Setup
    // withFolderProperties requires jenkins agent context to work so we need to execute it inside node block
    node('master') {
        withFolderProperties {
            gitHelper = new GitHelper(this, "${env.projectRepoHttpUrl}", "${env.gitCredentialsId}")

            def buildFlavorConfigDef = libraryResource("com/adisong/jenkins/pipeline-library/config/build/flavor/${params.buildFlavor}.json")
            buildFlavorConfig = jsonParse("${buildFlavorConfigDef}")
        }
    }

    pipeline {
        agent { label "${params.agentLabel}" }
        stages {
            stage ('configure and publish release branch'){
                steps {
                    script {
                        withEnv([
                            "RELEASE_VERSION=${env.releaseVersion}"
                        ]){
                            sh '''
                            git checkout -B release/${RELEASE_VERSION}
                            echo ${RELEASE_VERSION} > version
                            '''
                            if ( buildFlavorConfig.steps.release != null) {
                                sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${buildFlavorConfig.steps.release.target} ${buildFlavorConfig.steps.release.get('additionalFlags','')} ${buildFlavorConfig.steps.release.versionFlag}=${RELEASE_VERSION}"
                            }
                            sh '''
                            git add .
                            git commit -m "[Release] Prepare release ${RELEASE_VERSION}"                       
                            '''
                            gitHelper.pushRev("release/${RELEASE_VERSION}")
                            gitHelper.publishNextRCTag()
                        }
                    }
                }
            }
            stage ('bump version on develop branch') {
                steps {
                    script {
                        withEnv([
                            "NEW_VERSION=${env.newVersion}"
                        ]){
                            sh '''
                            git checkout develop
                            echo ${NEW_VERSION} > version
                            '''
                            if ( buildFlavorConfig.steps.release != null) {
                                sh "${buildFlavorConfig.buildTool} ${buildFlavorConfig.debugFlags} ${buildFlavorConfig.steps.release.target} ${buildFlavorConfig.steps.release.get('additionalFlags','')} ${buildFlavorConfig.steps.release.versionFlag}=${NEW_VERSION}"
                            }
                            sh '''
                            git add .
                            git commit -m "[Release] Prepare for new development iteration ${NEW_VERSION}"
                            '''
                            gitHelper.pushRev("develop")
                        }
                    }
                }
            }
        }
    }
}
