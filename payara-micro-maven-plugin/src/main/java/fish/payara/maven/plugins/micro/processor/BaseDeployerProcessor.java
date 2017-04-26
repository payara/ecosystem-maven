package fish.payara.maven.plugins.micro.processor;

import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

/**
 * @author mertcaliskan
 */
abstract class BaseDeployerProcessor extends BaseProcessor {

    protected MojoExecutor.Element[] constructElementsFromGivenArtifactItems(List<ArtifactItem> artifactItems) {
        List<MojoExecutor.Element> elements = new ArrayList<MojoExecutor.Element>();
        for (ArtifactItem artifactItem : artifactItems) {
            MojoExecutor.Element element = element(name("artifactItem"),
                    element("groupId", artifactItem.getGroupId()),
                    element("artifactId", artifactItem.getArtifactId()),
                    element("version", artifactItem.getVersion()),
                    element("type", artifactItem.getType())
            );
            elements.add(element);
        }
        return elements.toArray(new MojoExecutor.Element[elements.size()]);
    }
}