pipelineJob("jenkins-pipeline-library/applicationPipelinesDSLSeed"){
    logRotator{
        artifactDaysToKeep(7)
        daysToKeep(7)
    }
    definition {
        cpsScm {
            lightweight(true)
            scm {
                git {
                    branch('origin/${pipelineLibraryVersion}')
                    extensions {
                        wipeOutWorkspace()
                    }
                    remote {
                        // Sets credentials for authentication with the remote repository.
                        credentials("${gitCredentialsId}")
                        url("${pipelineLibraryRepoHttpUrl}")
                    }
                }
            }
            scriptPath("jenkins/applicationPipelinesDSLSeed.job")
        }
    }
    parameters {
        stringParam('projectName', '', 'Project name')
        stringParam('projectRepoHttpUrl', '', 'Project repository URL')
        stringParam('pipelineLibraryRepoHttpUrl', "${pipelineLibraryRepoHttpUrl}", 'Pipeline library repository URL')
        stringParam('pipelineLibraryVersion', "${pipelineLibraryVersion}", 'Default version of pipeline library' )
        stringParam('gitlabSecretToken', '', 'Gitlab token for webhooks')
        credentialsParam('gitCredentialsId'){
            type('com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl')
            required()
            defaultValue("${gitCredentialsId}")
            description('Credentials id with pull access to gitlab repos')
        }
        activeChoiceParam('gitlabConnection') {
            description('Gitlab connection name')
            choiceType('SINGLE_SELECT')
            groovyScript {
                script('''
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig
import jenkins.model.Jenkins
def connectionConfig = Jenkins.getInstanceOrNull().getDescriptor(GitLabConnectionConfig.class)
def choices = []
connectionConfig.connections.each { it ->
    def name = it.getName()
    if (name != '') {
        choices.add(name)
    }
}
return choices
                ''')
            }
        }
        activeChoiceParam('sonarInstallationName') {
            description('Sonarqube installation name')
            choiceType('SINGLE_SELECT')
            groovyScript {
                script('''
import hudson.plugins.sonar.SonarGlobalConfiguration
import jenkins.model.Jenkins
def sonarInstallations = Jenkins.getInstanceOrNull().getDescriptor(SonarGlobalConfiguration.class).getInstallations()
def choices = []
sonarInstallations.each { it ->
    def name = it.getName()
    if (name != ''){
        choices.add(name)
    }
}
return choices
                ''')
            }
        }
        stringParam('dockerAgentLabel', '', 'Jenkins agent label for image build steps')
        stringParam('baseImageRegistry', '', 'Docker registry url for base images')
        choiceParam('baseImageRegistryProto', ['http','https'], 'Base images docker registry protocol')
        credentialsParam('baseImageRegistryCredentialsId'){
            type('com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl')
            required()
            defaultValue("")
            description('Base images docker registry credentials with pull permission')
        }
        stringParam('destImageRegistry', '', 'Docker registry url for build images')
        choiceParam('destImageRegistryProto', ['http','https'], 'Build images docker registry protocol')
        credentialsParam('destImageRegistryCredentialsId'){
            type('com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl')
            required()
            defaultValue("")
            description('Build images docker registry credentials with push permission')
        }
        stringParam('destImageRegistryNamespace', '', 'Namespace in docker registry for images')
        stringParam('deployAgentLabel', '', 'Jenkins agent label for deploy steps')
        credentialsParam('k8sCredentialsId'){
            type('org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl')
            required()
            description('Kubernetes kubeconfig credentials id')
        }
        stringParam('k8sDeploymentName', '', 'Kubernetes deployment name')
        stringParam('k8sContainerName', '', 'Kubernetes app container name within deployment')
    }
    configure { project ->
        project / 'properties' / 'org.jenkinsci.plugins.authorizeproject.AuthorizeProjectProperty'(plugin: "authorize-project@${authorizeProjectVersion}") {
            strategy(class: 'org.jenkinsci.plugins.authorizeproject.strategy.TriggeringUsersAuthorizationStrategy')
        }
    }
}