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

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GetToken extends AbstractKerberos {


    public GetToken(File properties) {
    }

    private static String find(String prefix, String[] args) {
        for (String curr : args) {
            if (curr.startsWith(prefix)) {
                return curr.substring(prefix.length());
            }
        }
        return null;
    }

    public static void main(String[] args) throws LoginException, IOException {

        String securityProperties = find("--security-properties-file=", args);
        if (securityProperties == null) {
            System.out.println("--security-properties-file=<gfsecurity.properties> is a required property.");
            System.exit(1);
        }

        KerberosAuthInitializer authInitializer = new KerberosAuthInitializer();

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(securityProperties)));
        Properties secProps = authInitializer.getCredentials(properties, null, false);
        System.out.println(secProps.getProperty("security-token"));
    }
}
