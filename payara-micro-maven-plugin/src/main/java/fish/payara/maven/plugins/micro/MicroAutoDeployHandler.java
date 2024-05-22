/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.maven.plugins.micro;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;

/**
 *
 * @author Gaurav Gupta
 */
public class MicroAutoDeployHandler extends AutoDeployHandler {

    private final StartMojo start;

    public MicroAutoDeployHandler(StartMojo start, File webappDirectory) {
        super(start, webappDirectory);
        this.start = start;

    }

    @Override
    public void reload(boolean rebootRequired) {
        if (rebootRequired) {
            if (start.getMicroProcess().isAlive()) {
                WebDriverFactory.updateTitle("Restarting", project, start.getDriver(), log);
                start.getMicroProcess().destroy();
            }
        } else {
            WebDriverFactory.updateTitle(RELOADING, project, start.getDriver(), log);
            ReloadMojo reloadMojo = new ReloadMojo(project, log);
            reloadMojo.setDevMode(true);
            if (start.contextRoot != null) {
                reloadMojo.setContextRoot(start.contextRoot);
            }
            reloadMojo.setKeepState(start.keepState);
            if (start.hotDeploy) {
                Path rootPath = project.getBasedir().toPath();
                List<String> sourcesChanged = new ArrayList<>();
                reloadMojo.setHotDeploy(start.hotDeploy);
                for (Source source : sourceUpdatedPending) {
                    String extension = source.path.toString().substring(source.path.toString().lastIndexOf('.') + 1);
                    if (extension.equals("xml") || extension.equals("properties")) {
                        reloadMojo.setMetadataChanged(true);
                    }
                    Path relativePath = rootPath.relativize(source.path);
                    sourcesChanged.add(relativePath.toString().replace(File.separator, "/"));
                }
                log.debug("SourcesChanged: " + sourcesChanged);
                reloadMojo.setSourcesChanged(String.join(", ", sourcesChanged));
            }
            try {
                reloadMojo.execute();
            } catch (MojoExecutionException ex) {
                log.error("Error invoking Reload", ex);
            }

        }
    }

}
