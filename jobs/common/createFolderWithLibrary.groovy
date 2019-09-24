//Envs
//- projectName
//- pipelineLibraryVersion
//- pipelineLibraryRepoHttpUrl
//- gitCredentialsId

folder("${projectName}") {
    properties {
        folderProperties {
            properties {
                stringProperty{
                    key('projectRepoHttpUrl')
                    value("${projectRepoHttpUrl}")
                }
                stringProperty{
                    key('gitCredentialsId')
                    value("${gitCredentialsId}")
                }
                stringProperty{
                    key('gitlabConnection')
                    value("${gitlabConnection}")
                }
                stringProperty {
                    key('sonarInstallationName')
                    value("${sonarInstallationName}")
                }
                stringProperty {
                    key('dockerAgentLabel')
                    value("${dockerAgentLabel}")
                }
                stringProperty{
                    key('baseImageRegistry')
                    value("${baseImageRegistry}")
                }
                stringProperty{
                    key('baseImageRegistryProto')
                    value("${baseImageRegistryProto}")
                }
                stringProperty{
                    key('baseImageRegistryCredentialsId')
                    value("${baseImageRegistryCredentialsId}")
                }
                stringProperty{
                    key('destImageRegistry')
                    value("${destImageRegistry}")
                }
                stringProperty{
                    key('destImageRegistryProto')
                    value("${destImageRegistryProto}")
                }
                stringProperty{
                    key('destImageRegistryCredentialsId')
                    value("${destImageRegistryCredentialsId}")
                }
                stringProperty{
                    key('destImageRegistryNamespace')
                    value("${destImageRegistryNamespace}")
                }
                stringProperty {
                    key('deployAgentLabel')
                    value("${deployAgentLabel}")
                }
                stringProperty {
                    key('k8sCredentialsId')
                    value("${k8sCredentialsId}")
                }
                stringProperty {
                    key('k8sDeploymentName')
                    value("${k8sDeploymentName}")
                }
                stringProperty {
                    key('k8sContainerName')
                    value("${k8sContainerName}")
                }
            }
        }
        folderLibraries {
            libraries {
                libraryConfiguration {
                    name("jenkins-pipeline-library")
                    implicit(true)
                    defaultVersion("${pipelineLibraryVersion}")
                    retriever {
                        modernSCM {
                            scm {
                                git {
                                    remote("${pipelineLibraryRepoHttpUrl}")
                                    credentialsId("${gitCredentialsId}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
