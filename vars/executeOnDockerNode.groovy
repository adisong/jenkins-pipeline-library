def call(def params = [:]) {
    pipeline {
        agent { label "$params.agentLabel" }
        stages {
            stage('execute'){
                steps{
                    script {
                        params.commands.each {
                            command ->
                                sh "$command"
                        }
                    }
                }
            }
        }
    }
}
