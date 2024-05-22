/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.maven.plugins.cloud;

import fish.payara.maven.plugins.micro.AutoDeployHandler;
import fish.payara.maven.plugins.micro.WebDriverFactory;
import java.io.File;

/**
 *
 * @author Gaurav Gupta
 */
public class CloudAutoDeployHandler extends AutoDeployHandler {

    private final DevMojo start;

    public CloudAutoDeployHandler(DevMojo start, File webappDirectory) {
        super(start, webappDirectory);
        this.start = start;

    }

    @Override
    public void reload(boolean rebootRequired) {
        try {
            WebDriverFactory.updateTitle(RELOADING, project, start.getDriver(), log);
            start.deploy();
        } catch (Exception ex) {
            log.error("Error invoking Reload", ex);
        }
    }

}
