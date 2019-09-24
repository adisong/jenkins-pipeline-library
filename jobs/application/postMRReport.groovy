//Envs
//- projectName
//- projectRepoHttpUrl
//- gitlabSecretToken
//- gitCredentialsId

pipelineJob("${projectName}/postMRReport"){
    logRotator{
        artifactDaysToKeep(7)
        daysToKeep(7)
    }
    definition {
        cpsScm {
            lightweight(true)
            scm {
                git {
                    branch('origin/${gitlabTargetBranch}')
                    extensions {
                        wipeOutWorkspace()
                    }
                    remote {
                        // Sets credentials for authentication with the remote repository.
                        credentials("${gitCredentialsId}")
                        url("${projectRepoHttpUrl}")
                    }
                }
            }
            scriptPath("jenkins/postMRReport.job")
        }
    }
    triggers {
        gitlab {
            triggerOnAcceptedMergeRequest(true)
            triggerOnPush(false)
            triggerOnMergeRequest(false)
            triggerOnNoteRequest(false)
            secretToken("${gitlabSecretToken}")
        }
    }
}
