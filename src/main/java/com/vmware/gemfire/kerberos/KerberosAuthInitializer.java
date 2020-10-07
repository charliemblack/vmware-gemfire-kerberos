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
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.security.AuthInitialize;
import org.apache.geode.security.AuthenticationFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.Properties;

public class KerberosAuthInitializer extends AbstractKerberos implements AuthInitialize {
    public static final String JAAS_NAME = "security-kerberos-auth-jaas-name";
    public static final String LOGIN_USERNAME = "security-kerberos-auth-login-context-username";
    public static final String LOGIN_PASSWORD = "security-kerberos-auth-login-context-password";
    public static final String SERVICE_NAME = "security-kerberos-auth-service-name";
    public static final String LIFE_TIME = "security-kerberos-auth-context-lifetime";

    private static final Logger logger = LogManager.getLogger();
    private static final Object lock = new Object();
    // Making the Kerberos initial login a class variable to keep the cost of building a connection per instantiation down.
    private static Subject subject = null;
    private static String serviceName;
    private static int lifeTime = Integer.MAX_VALUE;

    private void atomicInitialize(Properties properties) throws AuthenticationFailedException {
        if (subject != null) {
            return;
        }
        synchronized (lock) {
            if (subject != null) {
                //another thread initialized first, exit.
                return;
            }
            subject = initializeSubject(properties.getProperty(JAAS_NAME),
                    getCallbackHandler(
                            properties.getProperty(LOGIN_USERNAME),
                            properties.getProperty(LOGIN_PASSWORD)
                    )
            );
            serviceName = properties.getProperty(SERVICE_NAME);
            if (StringUtils.isEmpty(serviceName)) {
                throw new AuthenticationFailedException("KerberosAuthInitializer: " + SERVICE_NAME + " can not be empty");
            }
            if (properties.getProperty(LIFE_TIME) != null) {
                lifeTime = Integer.parseInt(properties.getProperty(LIFE_TIME));
            }
        }
    }

    @Override
    public Properties getCredentials(Properties securityProps, DistributedMember server, boolean isPeer) throws AuthenticationFailedException {
        Properties properties = null;
        try {
            atomicInitialize(securityProps);
            properties = new Properties();
            properties.setProperty(KerberosSecurityManager.TOKEN_PROPERTY, getKerberosToken());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            subject = null;
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        return properties;
    }

    // Begin the initiation of a security context with the target service.
    private String getKerberosToken() throws AuthenticationFailedException {
        byte[] serviceTicket = null;
        try {
            GSSManager manager = GSSManager.getInstance();
            GSSName gssName = manager.createName(serviceName, GSSName.NT_HOSTBASED_SERVICE);
            final GSSContext context = manager.createContext(gssName, KerberosSecurityManager.KRB5_OID, null, lifeTime);
            // The GSS context initiation has to be performed as a privileged action.
            serviceTicket = Subject.doAs(subject, new PrivilegedAction<byte[]>() {
                public byte[] run() {
                    try {
                        byte[] token = new byte[0];
                        // This is a one pass context initialisation.
                        context.requestMutualAuth(false);
                        context.requestCredDeleg(false);
                        return context.initSecContext(token, 0, token.length);
                    } catch (GSSException e) {
                        subject = null;
                        logger.error(e.getMessage(), e);
                        throw new AuthenticationFailedException(e.getMessage(), e);
                    }
                }
            });
        } catch (GSSException e) {
            subject = null;
            logger.error(e.getMessage(), e);
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        String returnValue = null;
        if (serviceTicket != null) {
            returnValue = Base64.getEncoder().encodeToString(serviceTicket);
        }
        return returnValue;
    }
}
