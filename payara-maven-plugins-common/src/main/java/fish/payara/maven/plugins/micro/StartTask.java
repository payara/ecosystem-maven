/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package fish.payara.maven.plugins.micro;

import java.util.List;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.logging.Log;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author Gaurav Gupta
 */
public interface StartTask {

    MavenProject getProject();

    Log getLog();
    
    List<String> getRebootOnChange();
    
     WebDriver getDriver();
     
     boolean isLocal();
}
