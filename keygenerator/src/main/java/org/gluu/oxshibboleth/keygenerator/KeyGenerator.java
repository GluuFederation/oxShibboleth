/*
 * KeyGenerator is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.oxshibboleth.keygenerator;

import java.io.File;
//import java.security.Security;
//import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * BC Wrapper class
 */
public class KeyGenerator {
    
    public static void main(String args[]) {
        String path = ".";
        String masterPassword = "test";
        
        if (args.length >= 2) {
            path = args[0];
            masterPassword = args[1];
        }
        
        // init provider
        //Security.addProvider(new BouncyCastleProvider());
        //ConfigurableProvider prov = (ConfigurableProvider)Security.getProvider("BC");
        
        try {
            Keystore keystore = new Keystore(path + File.separator + "sealer.jks", masterPassword, Keystore.KEYSTORE_JCEKS);
            keystore.addKey(SymmetricKeyGenerator.generateKey() , "secret1", masterPassword);
            keystore.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

}
