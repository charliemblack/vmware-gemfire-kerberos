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
import org.apache.geode.security.SecurityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ietf.jgss.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.Properties;

public class KerberosSecurityManager extends AbstractKerberos implements SecurityManager {
    public static final String JAAS_NAME = "security-kerberos-security-manager-jaas-name";
    public static final String LOGIN_USERNAME = "security-kerberos-security-manager-login-context-username";
    public static final String LOGIN_PASSWORD = "security-kerberos-security-manager-login-context-password";
    public static final String SERVICE_NAME = "security-kerberos-security-manager-service-name";
    public static final String LIFE_TIME = "security-kerberos-security-manager-context-lifetime";

    public static final String TOKEN_PROPERTY = "security-token";
    public static final Oid KRB5_OID = createKRB5Oid();
    private static final Logger logger = LogManager.getLogger();
    private final Object lock = new Object();

    private CallbackHandler callbackHandler;
    private String jaasName;
    private Subject subject;

    private static Oid createKRB5Oid() {
        Oid returnValue = null;
        try {
            returnValue = new Oid("1.2.840.113554.1.2.2");
        } catch (GSSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return returnValue;
    }

    @Override
    public void init(Properties securityProps) {
        jaasName = securityProps.getProperty(JAAS_NAME);
        callbackHandler = getCallbackHandler(
                securityProps.getProperty(LOGIN_USERNAME),
                securityProps.getProperty(LOGIN_PASSWORD)
        );
        try {
            atomicInitialize();
        } catch (AuthenticationFailedException e) {
            subject = null;
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void atomicInitialize() throws AuthenticationFailedException {
        if (subject != null) {
            return;
        }
        synchronized (lock) {
            if (subject != null) {
                // If subject is not null another thread beat this thread here exit quickly.
                return;
            }
            subject = initializeSubject(jaasName, callbackHandler);
        }
    }

    @Override
    public Object authenticate(Properties credentials) throws AuthenticationFailedException {
        String token = null;
        try {
            token = credentials.getProperty(TOKEN_PROPERTY);
            if (StringUtils.isNotEmpty(token)) {
                return acceptToken(Base64.getDecoder().decode(token));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        throw new AuthenticationFailedException("Token: is the token null - " + (token == null));
    }

    @Override
    public void close() {

    }

    private String acceptToken(final byte[] serviceTicket) throws AuthenticationFailedException {
        atomicInitialize();
        return Subject.doAs(subject, (PrivilegedAction<String>) () -> {
            try {
                // Identify the server that communications are being made to.
                GSSManager manager = GSSManager.getInstance();
                GSSContext context = manager.createContext((GSSCredential) null);
                context.acceptSecContext(serviceTicket, 0, serviceTicket.length);
                String serviceName = context.getSrcName().toString();
                context.isEstablished();
                context.dispose();
                return serviceName;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new AuthenticationFailedException(e.getMessage(), e);
            }
        });
    }
}
