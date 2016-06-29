/*
 * KeyGenerator is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.xdi.oxshibboleth.keygenerator;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * AES-128
 */
public class SymmetricKeyGenerator {
    /**
     * Generates a secret key to be used in the encryption process.
     * 
     * @return The secret key
     */
    public static SecretKey generateKey() {
        KeyGenerator keygen;
        try {

            //Java normally doesn't support 256-bit key sizes without an extra installation so stick with a 128-bit key
            keygen = KeyGenerator.getInstance("AES");
            keygen.init(128);
            SecretKey aesKey = keygen.generateKey();
            return aesKey;
        }
        catch(NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
