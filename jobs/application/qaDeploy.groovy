//Envs
//- projectName
//- projectRepoHttpUrl
//- gitlabSecretToken
//- gitCredentialsId

pipelineJob("${projectName}/qaDeploy"){
    logRotator{
        artifactDaysToKeep(7)
        daysToKeep(7)
    }
    definition {
        cpsScm {
            lightweight(false)
            scm {
                git {
                    branch('${gitlabBranch}')
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
            scriptPath("jenkins/qaDeploy.job")
        }
    }
    triggers {
        gitlab {
            triggerOnPush(true)
            branchFilterType('RegexBasedFilter')
            sourceBranchRegex(/^refs\/tags\/\d+\.\d+\.\d+-RC\.\d+$/)
            triggerOnMergeRequest(false)
            triggerOnApprovedMergeRequest(false)
            triggerOnNoteRequest(false)
            secretToken("${gitlabSecretToken}")
        }
    }
}