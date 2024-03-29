/*
 *
 * Copyright (c) 2017-2024 Payara Foundation and/or its affiliates. All rights reserved.
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

/**
 * @author mertcaliskan
 */
public interface Configuration {
    String MICRO_GROUPID = "fish.payara.extras";
    String MICRO_ARTIFACTID = "payara-micro";

    String EXTRACTED_PAYARAMICRO_FOLDER = File.separator + "extracted-payaramicro";
    String MICROINF_FOLDER = File.separator + "MICRO-INF";

    String MICROINF_DEPLOY_FOLDER = MICROINF_FOLDER + File.separator + "deploy";
    String MICROINF_LIB_FOLDER = MICROINF_FOLDER + File.separator + "lib";
    String MICROINF_DOMAIN_FOLDER = MICROINF_FOLDER + File.separator + "domain";
    String OUTPUT_FOLDER = "${project.build.directory}" + EXTRACTED_PAYARAMICRO_FOLDER;
    String METAINF_FOLDER = OUTPUT_FOLDER + File.separator + "META-INF";

    String JAVA_EXECUTABLE = "java";
    String JAR_EXTENSION = "jar";
    String WAR_EXTENSION = "war";
    String MICROBUNDLE_EXTENSION = "microbundle";

    String MICRO_THREAD_NAME = "PayaraMicroThread";
    String MICRO_READY_MESSAGE = "ready in";
    
    // Log parser
    String INSTANCE_CONFIGURATION = " \"Instance Configuration\": {";
    String HOST_IP_PATTERN = "\"Host\": \"([^\"]+)\"";
    String HOST_PORT_PATTERN = "\"Http Port\\(s\\)\": \"([^\"]+)\"";
    String PAYARA_MICRO_URLS = "Payara Micro URLs:";
    String LOADING_APPLICATION = "Loading application";
    String LOADING_APPLICATION_PATTERN = "Loading application \\[([^\\]]+)\\] at \\[([^\\]]+)\\]";
    String APP_DEPLOYED = "was successfully deployed";
    String APP_DEPLOYED_PATTERN = "([^ ]+) was successfully deployed in ([^ ]+) milliseconds.";
    String INOTIFY_USER_LIMIT_REACHED_MESSAGE = "User limit of inotify instances reached";
    String WATCH_SERVICE_ERROR_MESSAGE = "Error starting WatchService. User limit of inotify instances reached or too many open files. Please increase the max_user_watches configuration.";
    String APP_DEPLOYMENT_FAILED = "Exception while loading the app";
    String APP_DEPLOYMENT_FAILED_MESSAGE = "Failed reloading";

}
