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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.callback.*;
import java.io.IOException;

public class LoginCallbackHandler implements CallbackHandler {

    private static final Logger logger = LogManager.getLogger();
    private String password;
    private String username;

    public LoginCallbackHandler() {
        super();
    }

    public LoginCallbackHandler(String name, String password) {
        this.username = name;
        this.password = password;
    }

    public LoginCallbackHandler(String password) {
        this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback && username != null) {
                NameCallback nameCallback = (NameCallback) callbacks[i];
                nameCallback.setName(username);
            } else if (callbacks[i] instanceof PasswordCallback && password != null) {
                PasswordCallback passwordCallback = (PasswordCallback) callbacks[i];
                passwordCallback.setPassword(password.toCharArray());
            }
        }
    }
}
