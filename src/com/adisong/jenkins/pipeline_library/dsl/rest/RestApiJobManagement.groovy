package com.adisong.jenkins.pipeline_library.dsl.rest

import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import javaposse.jobdsl.dsl.*

class RestApiJobManagement extends AbstractJobManagement {

    final RESTClient restClient
    private boolean crumbHeaderSet = false
    private final Map<String, Object> envVars
    final Map<String, List<String>> permissions = [
            'hudson.security.AuthorizationMatrixProperty': [
                    'hudson.model.Item.Delete',
                    'hudson.model.Item.Configure',
                    'hudson.model.Item.Read',
                    'hudson.model.Item.Discover',
                    'hudson.model.Item.Build',
                    'hudson.model.Item.Workspace',
                    'hudson.model.Item.Cancel',
                    'hudson.model.Item.Release',
                    'hudson.model.Item.ExtendedRead',
                    'hudson.model.Run.Delete',
                    'hudson.model.Run.Update',
                    'hudson.scm.SCM.Tag'
            ]
    ]


    RestApiJobManagement(String baseUrl, Map<String, Object> envVars) {
        super(System.out)
        restClient = new RESTClient(baseUrl)
        restClient.handler.failure = { it }
        this.envVars = envVars
    }

    RestApiJobManagement(String baseUrl) {
        this(baseUrl, null)
    }

    void setCredentials(String username, String password) {
        crumbHeaderSet = false
        restClient.headers['Authorization'] = 'Basic ' + "$username:$password".bytes.encodeBase64()
    }

    @Override
    String getConfig(String jobName) throws JobConfigurationNotFoundException {
        String xml = fetchExistingXml(jobName)
        if (!xml) {
            throw new JobConfigurationNotFoundException(jobName)
        }

        xml
    }

    @Override
    Map<String, Object> getParameters() {
        return envVars
    }

    @Override
    boolean createOrUpdateConfig(Item item, boolean ignoreExisting) throws NameNotProvidedException {
        createOrUpdateConfig(item.name, item.xml, ignoreExisting, false)
    }

    @Override
    void createOrUpdateView(String viewName, String config, boolean ignoreExisting) throws NameNotProvidedException, ConfigurationMissingException {
        createOrUpdateConfig(viewName, config, ignoreExisting, true)
    }

    boolean createOrUpdateConfig(String name, String xml, boolean ignoreExisting, boolean isView) throws NameNotProvidedException {
        boolean success
        String status

        String existingXml = fetchExistingXml(name, isView)
        if (existingXml) {
            if (ignoreExisting) {
                success = true
                status = 'ignored'
            } else {
                success = update(name, xml, isView)
                status = success ? 'updated' : 'update failed'
            }
        } else {
            success = create(name, xml, isView)
            status = success ? 'created' : 'create failed'
        }

        println "$name - $status"
        success
    }

    @Override
    InputStream streamFileInWorkspace(String filePath) throws IOException {
        new File(filePath).newInputStream()
    }

    @Override
    String readFileInWorkspace(String filePath) throws IOException {
        new File(filePath).text
    }

    @Override
    void renameJobMatching(String previousNames, String destination) throws IOException {
    }

    @Override
    void queueJob(String jobName) throws NameNotProvidedException {
        validateNameArg(jobName)
    }

    @Override
    String readFileInWorkspace(String jobName, String filePath) throws IOException {
        throw new UnsupportedOperationException()
    }

    @Override
    void logPluginDeprecationWarning(String pluginShortName, String minimumVersion) {
    }

    @Override
    void requirePlugin(String pluginShortName, boolean failIfMissing) {
    }

    @Override
    void requireMinimumPluginVersion(String pluginShortName, String version, boolean failIfMissing) {
    }

    @Override
    void requireMinimumCoreVersion(String version) {
    }

    @Override
    boolean isMinimumPluginVersionInstalled(String pluginShortName, String version) {
        false
    }

    @Override
    boolean isMinimumCoreVersion(String version) {
        false
    }

    @Override
    Integer getVSphereCloudHash(String name) {
        null
    }

    @Override
    Set<String> getPermissions(String authorizationMatrixPropertyClassName) {
        permissions[authorizationMatrixPropertyClassName] ?: []
    }

    @Override
    Node callExtension(String name, Item item, Class<? extends ExtensibleContext> contextType,
                       Object... args) {
        null
    }

    @Override
    void createOrUpdateUserContent(UserContent userContent, boolean ignoreExisting) {
    }

    private boolean create(String name, String xml, boolean isView) {
        String job
        String path
        if (name.contains('/')) {
            int index = name.lastIndexOf('/')
            String folder = name[0..(index - 1)]
            job = name[(index + 1)..-1]
            path = getPath(folder, isView) + '/createItem'
        } else {
            job = name
            path = isView ? 'createView' : 'createItem'
        }

        setCrumbHeader()
        HttpResponseDecorator resp = restClient.post(
                path: path,
                body: xml,
                query: [name: job],
                requestContentType: 'application/xml'
        )

        resp.status == 200
    }

    private boolean update(String name, String xml, boolean isView) {
        setCrumbHeader()
        HttpResponseDecorator resp = restClient.post(
                path: getPath(name, isView) + '/config.xml',
                body: xml,
                requestContentType: 'application/xml'
        )

        resp.status == 200
    }

    private String fetchExistingXml(String name, boolean isView) {
        setCrumbHeader()
        HttpResponseDecorator resp = restClient.get(
                contentType: ContentType.TEXT,
                path: getPath(name, isView) + '/config.xml',
                headers: [Accept: 'application/xml'],
        )
        resp?.data?.text
    }

    static String getPath(String name, boolean isView) {
        if (name.startsWith('/')) {
            return '/' + getPath(name[1..-1], isView)
        }
        isView ? "view/$name" : "job/${name.replaceAll('/', '/job/')}"
    }

    private setCrumbHeader() {
        if (crumbHeaderSet)
            return

        HttpResponseDecorator resp = restClient.get(path: 'crumbIssuer/api/xml')
        if (resp.status == 200) {
            restClient.headers[resp.data.crumbRequestField] = resp.data.crumb
        }
        crumbHeaderSet = true
    }
}
