/*
 *
 * Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.maven.plugins;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.InvokerLogger;

/**
 *
 * @author Gaurav Gupta
 */
public class InvokerLoggerImpl implements InvokerLogger {

    private final Log log;

    public InvokerLoggerImpl(Log log) {
        this.log = log;
    }

    @Override
    public void debug(String val) {
        log.debug(val);
    }

    @Override
    public void debug(String val, Throwable thrwbl) {
        log.debug(val, thrwbl);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void info(String val) {
        log.info(val);
    }

    @Override
    public void info(String val, Throwable thrwbl) {
        log.info(val, thrwbl);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void warn(String val) {
        log.warn(val);
    }

    @Override
    public void warn(String val, Throwable thrwbl) {
        log.warn(val, thrwbl);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void error(String val) {
        log.error(val);
    }

    @Override
    public void error(String val, Throwable thrwbl) {
        log.error(val, thrwbl);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void fatalError(String val) {
        log.error(val); // Assuming fatalError is treated as an error
    }

    @Override
    public void fatalError(String val, Throwable thrwbl) {
        log.error(val, thrwbl);
    }

    @Override
    public boolean isFatalErrorEnabled() {
        return true;
    }

    @Override
    public void setThreshold(int threshold) {
    }

    @Override
    public int getThreshold() {
        // Implement threshold retrieval if needed
        return 0;
    }
}
