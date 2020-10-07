/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.vmware.gemfire.kerberos;

import org.apache.commons.lang3.StringUtils;
import org.apache.geode.security.AuthenticationFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class AbstractKerberos {
    protected static final Logger logger = LogManager.getLogger();

    protected Subject initializeSubject(String jaasName, CallbackHandler callbackHandler) throws AuthenticationFailedException {
        Subject subject = null;
        try {
            LoginContext loginContext = null;
            if (callbackHandler == null) {
                loginContext = new LoginContext(jaasName);
            } else {
                loginContext = new LoginContext(jaasName, callbackHandler);
            }
            loginContext.login();
            subject = loginContext.getSubject();
        } catch (LoginException e) {
            logger.error(e.getMessage(), e);
            subject = null;
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        return subject;
    }

    protected CallbackHandler getCallbackHandler(String username, String password) {
        CallbackHandler callbackHandler = null;
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            callbackHandler = new LoginCallbackHandler(username, password);
        } else if (StringUtils.isNotEmpty(password)) {
            callbackHandler = new LoginCallbackHandler(password);
        }
        return callbackHandler;
    }
}
