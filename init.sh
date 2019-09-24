#!/bin/bash
cd "${0%/*}"

#############
#
# Usage:
# ./init.sh
#
# ENV
#   - JENKINS_USER
#   - JENKINS_PASSWORD
#   - JENKINS_HOST
#
#############

source .env
baseURL=${JENKINS_HOST:-http://localhost:8080/}

# Folder creation first
scripts=(jobs/init/createFolderForDSLSeedJobs.groovy jobs/init/applicationPipelinesDSLSeed.groovy)

for script in ${scripts[@]}; do
    ./gradlew rest \
        -Dpattern=${script} \
        -DbaseUrl=${baseURL} \
        -Dusername=${JENKINS_USER} \
        -Dpassword=${JENKINS_PASSWORD} \
        -Ddsl.pipelineLibraryRepoHttpUrl=$(git remote get-url origin) \
        -Ddsl.pipelineLibraryVersion=$(git rev-parse --abbrev-ref HEAD) \
        -Ddsl.gitCredentialsId=gitlab-repo-access \
        -Ddsl.authorizeProjectVersion=1.3.0
done





