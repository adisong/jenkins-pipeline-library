package helpers

class JobDSLTestHelper {

    static List<File> getJobFiles() {
        //Order of execution matters as some jobs are creating folders
        def scripts = [
                "jobs/init/createFolderForDSLSeedJobs.groovy",
                "jobs/init/applicationPipelinesDSLSeed.groovy",
                "jobs/common/createFolderWithLibrary.groovy",
                "jobs/application/mrVerify.groovy",
                "jobs/application/postMRReport.groovy",
                "jobs/application/prepareRelease.groovy",
                "jobs/application/devDeploy.groovy",
                "jobs/application/qaDeploy.groovy",
                "jobs/application/prodDeploy.groovy"
        ]

        List<File> files = []
        scripts.each {
            files << new File(it)
        }
        files
    }

    /**
     * Write a single XML file, creating any nested dirs.
     */
    static void writeFile(File dir, String name, String xml) {
        List tokens = name.split('/')
        File folderDir = tokens[0..<-1].inject(dir) { File tokenDir, String token ->
            new File(tokenDir, token)
        }
        folderDir.mkdirs()

        File xmlFile = new File(folderDir, "${tokens[-1]}.xml")
        xmlFile.text = xml
    }
}
