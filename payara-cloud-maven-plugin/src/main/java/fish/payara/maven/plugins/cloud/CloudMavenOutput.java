/*
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.maven.plugins.cloud;

import fish.payara.cloud.client.ClientOutput;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 *
 * @author Gaurav Gupta
 */
public class CloudMavenOutput implements ClientOutput {

    private final org.apache.maven.plugin.logging.Log LOG;

    private final boolean intractive;

    public CloudMavenOutput(org.apache.maven.plugin.logging.Log log, boolean intractive) {
        this.LOG = log;
        this.intractive = intractive;
    }
    
    @Override
    public void warning(String message) {
        LOG.warn(message);
    }

    @Override
    public void info(String message) {
        LOG.info(message);
    }

    @Override
    public void error(String message, Throwable cause) {
        LOG.error(message, cause);
    }

    @Override
    public void started(Object processId, Runnable cancellation) {
        LOG.debug("Started: " + processId);
    }

    @Override
    public void progress(Object processId, String message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Progress: " + processId + " " + message);
        } else {
            LOG.info(message);
        }
    }

    @Override
    public void finished(Object processId) {
       LOG.debug("Finished: " + processId);
    }

    @Override
    public void openUrl(URI uri) {
        LOG.info("Opening URL: " + uri);
        if (intractive) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(uri);
                } else {
                    LOG.warn("Desktop browsing is not supported on this platform.");
                }
            } catch (IOException e) {
                LOG.error("Failed to open URL: " + uri, e);
            }
        }
    }

    @Override
    public void failure(String message, Throwable cause) {
        LOG.error("Failure: " + message, cause);
    }

}
