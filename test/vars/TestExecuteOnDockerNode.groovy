package vars

import helpers.PipelineSpockTestBase

/**
 * How to unit test some vars DSL like shared code
 */
class TestExecuteOnDockerNode extends PipelineSpockTestBase {

    def "default"() {

        given:
        def params = [agentLabel: 'maven-3.6.0', commands: ['mvn --version','mvn clean package']]

        when:
        def script = loadScript('vars/executeOnDockerNode.groovy')
        script.call(params)

        then:
        printCallStack()
        testNonRegression('default')
        assertJobStatusSuccess()
    }
}