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

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class Client {

    public static void main(String[] args) throws IOException {
        System.setProperty("java.security.krb5.realm", "GEMFIRE");
        System.setProperty("java.security.krb5.kdc", "localhost");
        System.setProperty("java.security.auth.login.config", "jaas.conf");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        System.out.println("System.getProperty(\"user.dir\");\n = " + System.getProperty("user.dir"));
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File("etc/gfsecurity-app.properties")));
        ClientCache clientCache = new ClientCacheFactory(properties)
                .addPoolLocator("localhost", 10334)
                .setPoolMinConnections(4)
                .setPoolMaxConnections(10)
                .create();
        final Region region = clientCache.createClientRegionFactory(ClientRegionShortcut.PROXY).create("test");
        while(true) {
            Runnable runnable = new Runnable() {
                public void run() {
                    region.put(1, 1);
                    System.out.println("region.get(1) = " + region.get(1));
                    System.out.println("new Date() = " + new Date());
                }
            };
            new Thread(runnable).start();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
