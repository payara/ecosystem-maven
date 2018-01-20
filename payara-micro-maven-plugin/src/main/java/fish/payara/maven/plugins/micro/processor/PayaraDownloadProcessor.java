package fish.payara.maven.plugins.micro.processor;

import org.apache.maven.plugin.MojoExecutionException;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @author pablobastidas
 */
public class PayaraDownloadProcessor extends BaseProcessor {

    private String url;

    @Override
    public void handle(ExecutionEnvironment environment) throws MojoExecutionException {
        executeMojo(downloadPlugin,
                goal("wget"),
                configuration(
                        element(name("url"), url),
                        element(name("outputFileName"), PAYARAMICRO_JAR_FILE),
                        element(name("outputDirectory"), JAR_OUTPUT_FOLDER)
                ),
                environment
        );

        gotoNext(environment);
    }

    public PayaraDownloadProcessor set(String parameter) {
        this.url = parameter;
        return this;
    }
}