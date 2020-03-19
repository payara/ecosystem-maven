/*
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.maven.plugins.micro;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import static org.apache.maven.repository.RepositorySystem.DEFAULT_LOCAL_REPO_ID;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;

public abstract class AbstractMojoTest extends AbstractMojoTestCase {

    protected AbstractMojoTest() {
        try {
            this.setUp();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private MavenProject createMavenProject(String pomPath) throws IOException {
        File OUTPUT_DIR = new File(getBasedir(), "target/test/micro-mojo");
        File pomFile = getTestFile("src/test/resources/" + pomPath);
        Model model = new DefaultModelReader().read(pomFile, Collections.emptyMap());
        model.getBuild().setDirectory(OUTPUT_DIR.getAbsolutePath());
        MavenProject project = new MavenProject(model);
        project.setFile(pomFile);
        return project;
    }

    protected <T> T getMojo(String pom, String execName, Class<T> clazz) throws Exception {
        MavenProject project = createMavenProject(pom);
        project.setPluginArtifactRepositories(Arrays.asList(getRemoteRepository()));
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution(execName);
        Mojo mojo = lookupConfiguredMojo(session, execution);
        assertNotNull(mojo);
        return clazz.cast(mojo);
    }

    @Override
    protected MavenSession newMavenSession(MavenProject project) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(getLocalRepository());
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        RepositorySystemSession systemSession = LegacyLocalRepositoryManager.overlay(getLocalRepository(), MavenRepositorySystemUtils.newSession(), null);
        MavenSession session = new MavenSession(
                this.getContainer(),
                systemSession,
                request,
                result
        );
        session.setCurrentProject(project);
        session.setProjects(Arrays.asList(project));
        return session;
    }

    protected ArtifactRepository getLocalRepository() {
        File localRepositoryDirectory;
        String localRepo = System.getProperty("maven.local.repo");
        if (StringUtils.isNotEmpty(localRepo)) {
            localRepositoryDirectory = new File(localRepo);
        } else {
            localRepositoryDirectory = new File(System.getProperty("user.home"), ".m2/repository");
        }
        return new MavenArtifactRepository(
                DEFAULT_LOCAL_REPO_ID,
                "file://" + localRepositoryDirectory.toURI().getPath(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy()
        );
    }

    protected ArtifactRepository getRemoteRepository() {
        return new MavenArtifactRepository(
                "id", "https://repo.maven.apache.org/maven2",
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy()
        );
    }

}
