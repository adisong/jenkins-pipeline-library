//Envs
//- projectName
//- projectRepoHttpUrl
//- gitlabSecretToken
//- gitCredentialsId

pipelineJob("${projectName}/prodDeploy"){
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
            scriptPath("jenkins/prodDeploy.job")
        }
    }
    triggers {
        gitlab {
            branchFilterType('RegexBasedFilter')
            sourceBranchRegex(/^(release|hotfix)\/\d+\.\d+\.\d+$/)
            targetBranchRegex(/^master$/)
            triggerOnAcceptedMergeRequest(true)
            triggerOnPush(false)
            triggerOnMergeRequest(false)
            triggerOnNoteRequest(false)
            secretToken("${gitlabSecretToken}")
        }
    }
}