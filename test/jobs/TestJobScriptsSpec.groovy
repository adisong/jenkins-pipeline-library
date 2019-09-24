import hudson.model.Item
import hudson.model.View
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.GeneratedJob
import javaposse.jobdsl.dsl.GeneratedView
import helpers.JobDSLTestHelper
import javaposse.jobdsl.plugin.JenkinsJobManagement
import jenkins.model.Jenkins
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class TestJobScriptsSpec extends Specification {
    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared
    private File outputDir = new File('./build/debug-xml')

    def setupSpec() {
        outputDir.deleteDir()
    }

    @Unroll
    def 'test script #file.name'(File file) {
        given:
        Map<String, String> envVars = [
                projectName: 'my-project',
                projectRepoHttpUrl: "https://git.mycompany.com/my-project",
                pipelineLibraryRepoHttpUrl: "https://git.mycompany.com/my-pipeline-library",
                pipelineLibraryVersion: "develop",
                authorizeProjectVersion: "1.3.0",
                gitlabSecretToken: "my-secret-token",
                gitCredentialsId: "gitlab-repo-access",
                gitlabConnection: "gitlab-connection",
                sonarInstallationName: "sonarqube",
                dockerAgentLabel: "docker-18.09-dind",
                baseImageRegistry: "localhost:5000",
                baseImageRegistryProto: "http",
                baseImageRegistryCredentialsId: "docker-registry-pull",
                destImageRegistry: "localhost:6000",
                destImageRegistryProto: "http",
                destImageRegistryCredentialsId: "docker-registry-push",
                destImageRegistryNamespace: "my-namespace",
                deployAgentLabel: "kubectl-1.12.6",
                k8sCredentialsId: "kubeconfig-credentials",
                k8sDeploymentName: "my-deployment",
                k8sContainerName: "main"
        ]
        def jobManagement = new JenkinsJobManagement(System.out, envVars, new File('.'))

        when:
        GeneratedItems items = new DslScriptLoader(jobManagement).runScript(file.text)
        writeItems(items, outputDir)

        then:
        noExceptionThrown()

        where:
        file << JobDSLTestHelper.getJobFiles()
    }

    /**
     * Write the config.xml for each generated job and view to the build dir.
     */
    private void writeItems(GeneratedItems items, File outputDir) {
        Jenkins jenkins = jenkinsRule.jenkins
        items.jobs.each { GeneratedJob generatedJob ->
            String jobName = generatedJob.jobName
            Item item = jenkins.getItemByFullName(jobName)
            String text = new URL(jenkins.rootUrl + item.url + 'config.xml').text
            JobDSLTestHelper.writeFile(new File(outputDir, 'jobs'), jobName, text)
        }

        items.views.each { GeneratedView generatedView ->
            String viewName = generatedView.name
            View view = jenkins.getView(viewName)
            String text = new URL(jenkins.rootUrl + view.url + 'config.xml').text
            JobDSLTestHelper.writeFile(new File(outputDir, 'views'), viewName, text)
        }
    }
}