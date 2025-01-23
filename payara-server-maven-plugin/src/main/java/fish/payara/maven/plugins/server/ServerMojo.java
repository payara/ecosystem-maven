/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.maven.plugins.server;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;

/**
 *
 * @author Gaurav Gupta
 */
public abstract class ServerMojo extends BasePayaraMojo {

    @Parameter(property = "javaPath")
    protected String javaPath;

    @Parameter(property = "payaraVersion", defaultValue = "6.2024.12")
    protected String payaraVersion;

    @Parameter(property = "payaraServerAbsolutePath")
    protected String payaraServerAbsolutePath;

    @Parameter(property = "artifactItem")
    protected ArtifactItem artifactItem;
}
