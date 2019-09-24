//Envs
//- projectName
//- projectRepoHttpUrl
//- gitlabSecretToken
//- gitCredentialsId

pipelineJob("${projectName}/mrVerify"){
    logRotator{
        artifactDaysToKeep(7)
        daysToKeep(7)
    }
    definition {
        cpsScm {
            lightweight(true)
            scm {
                git {
                    branch('origin/${gitlabSourceBranch}')
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
            scriptPath("jenkins/mrVerify.job")
        }
    }
    triggers {
        gitlab {
            triggerOnPush(false)
            triggerOnMergeRequest(true)
            // trigger on push to source or target branch
            triggerOpenMergeRequestOnPush('both')
            triggerOnNoteRequest(true)
            noteRegex('^CI retry$')
            secretToken("${gitlabSecretToken}")
        }
    }
}