//Envs
//- projectName
//- projectRepoHttpUrl
//- gitlabSecretToken
//- gitCredentialsId

pipelineJob("${projectName}/prepareRelease"){
    logRotator{
        artifactDaysToKeep(7)
        daysToKeep(7)
    }
    parameters {
        stringParam('releaseVersion','','What version should be on release branch')
        stringParam('newVersion','','What version should be on develop branch after release')
    }
    definition {
        cpsScm {
            lightweight(true)
            scm {
                git {
                    branch('origin/develop')
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
            scriptPath("jenkins/prepareRelease.job")
        }
    }
}