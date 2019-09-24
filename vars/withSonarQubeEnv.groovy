def call(String configName, Closure body){
    def sonarqubeParams = []
    if (env.gitlabActionType == 'MERGE' || env.gitlabActionType == 'NOTE') {
        def commit_sha = env.gitlabMergeRequestCommitsDiff != '' ? env.gitlabMergeRequestCommitsDiff : env.gitlabMergeRequestLastCommit
        sonarqubeParams = [
                "-Dsonar.gitlab.commit_sha=${commit_sha}",
                "-Dsonar.gitlab.unique_issue_per_inline=true",
                "-Dsonar.gitlab.ref_name=${env.gitlabSourceBranch}",
                "-Dsonar.gitlab.project_id=${env.gitlabMergeRequestTargetProjectId}"
        ]
    }
    if (body != null) {
        //Inject additional env variables
        withEnv([
                "SONAR_GITLAB_PARAMS=${sonarqubeParams.join(' ')}" //parameters for sonar-gitlab-plugin see https://github.com/gabrie-allaigre/sonar-gitlab-plugin
        ]){
            steps.withSonarQubeEnv("${configName}"){
                body.call()
            }
        }
    }
}