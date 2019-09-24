//Envs
//- projectName
//- projectRepoHttpUrl
//- gitlabSecretToken
//- gitCredentialsId

pipelineJob("${projectName}/devDeploy"){
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
            scriptPath("jenkins/devDeploy.job")
        }
    }
    triggers {
        gitlab {
            triggerOnPush(true)
            branchFilterType('NameBasedFilter')
            includeBranchesSpec('develop')
            triggerOnMergeRequest(false)
            triggerOnApprovedMergeRequest(false)
            triggerOnNoteRequest(false)
            secretToken("${gitlabSecretToken}")
        }
    }
}