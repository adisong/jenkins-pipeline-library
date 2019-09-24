# jenkins-pipeline-library

Jenkins pipeline library for scripted/declarative pipelines

## File structure

    .
    ├── jenkins                         # Seed jobs pipeline definitions
    ├── jobs                            # DSL script files
    │   ├── application
    │   ├── common
    │   ├── init                        # Seed job DSL specs (used in init script)
    │   └── jobDSL.gdsl                 # IDE support for job DSL
    ├── modules
    │   └── jenkins-global-library      # Jenkins global library git submodule
    ├── resources                       # Pipeline resource files
    ├── src                             # Groovy source files
    ├── test                            # Groovy test files
    │   ├── callstacks                  # Callstacks for pipeline regression testing
    │   ├── helpers                     # Test helpers
    │   ├── jobs                        # Job DSL Specs tests
    │   └── vars                        # Pipeline global vars tests
    ├── vars
    │   └── declarative.gdsl            # IDE support for declarative pipelines
    ├── .env.example                    # Sample config for init.sh script
    ├── README.md
    ├── build.gradle                    # Build script
    ├── init.sh                         # Init script to create seed job with REST runner
    └── settings.gradle

## Testing

### Jenkins Test Harness

This project uses [Jenkins test harness](https://github.com/jenkinsci/jenkins-test-harness) to execute some tests against local Jenkins instance with preinstalled plugins. 
Configuration is in [jenkins-test-harness.gradle](gradle/jenkins-test-harness.gradle).

```groovy
ext {
    pluginArtifacts = [
            // Add any plugin dependencies that Jenkins test instance should have to run properly here
            // eg. "org.jenkins-ci.plugins:cloudbees-folder:6.7"
    ]
}
```

### Job DSL

`./gradlew :test --tests TestJobScriptsSpec` runs the specs tests.

[TestJobScriptsSpec](test/jobs/TestJobScriptsSpec.groovy) 
will loop through all DSL files and make sure they don't throw any exceptions when processed. All XML output files are written to `build/debug-xml`. 
This can be useful if you want to inspect the generated XML before check-in. Plugins providing auto-generated DSL must be added to the build dependencies.

## Seed job

You can create initial seed job via the REST API Runner (see below) by executing:

```bash
./init.sh 
```

This script sources configuration from `.env` file (example at [.env.example](.env.example))

## REST API Runner

Note: the REST API Runner does not work with [Automatically Generated DSL](https://github.com/jenkinsci/job-dsl-plugin/wiki/Automatically-Generated-DSL). 

A gradle task is configured that can be used to create/update jobs via the Jenkins REST API, if desired. Normally
a seed job is used to keep jobs in sync with the DSL, but this runner might be useful if you'd rather process the
DSL outside of the Jenkins environment or if you want to create the seed job from a DSL script.
Configuration is in [jenkins-rest-runner.gradle](gradle/jenkins-rest-runner.gradle).

```./gradlew rest -Dpattern=<pattern> -DbaseUrl=<baseUrl> [-Dusername=<username>] [-Dpassword=<password>] [-Ddsl.<parameterName>=<value>]```

* `pattern` - ant-style path pattern of files to include. E.g. `src/jobs/*.groovy`
* `baseUrl` - base URL of Jenkins server
* `username` - Jenkins username, if secured
* `password` - Jenkins password or token, if secured
* `dsl.<parameterName>` - Any parameter prefixed with `dsl.` will be passed to corresponding Job DSL spec with this prefix removed
