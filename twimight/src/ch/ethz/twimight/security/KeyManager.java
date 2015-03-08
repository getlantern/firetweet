/*******************************************************************************
 * Copyright (c) 2011 ETH Zurich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Paolo Carta - Implementation
 *     Theus Hossmann - Implementation
 *     Dominik Schatzmann - Message specification
 ******************************************************************************/

package ch.ethz.twimight.security;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.spongycastle.jce.provider.X509CertificateObject;
import org.spongycastle.openssl.PEMReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import ch.ethz.twimight.data.FriendsKeysDBHelper;
import ch.ethz.twimight.util.Constants;

/**
 * Manages the cryptographic key.
 * @author thossmann
 *
 */
public class KeyManager {

    // The names of the fields in shared preferences
    private static final String PRIVATE_EXPONENT = "PRIVATE_exponent";
    private static final String PRIVATE_MODULUS = "PRIVATE_modulus";
    private static final String PUBLIC_EXPONENT = "PUBLIC_exponent";
    private static final String PUBLIC_MODULUS = "PUBLIC_modulus";

    private static final String TAG = "KeyManager"; /** Logging */
    SharedPreferences prefs;
    private Context context;

    /**
     * Constructor
     */
    public KeyManager(Context context){
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            this.context=context;
    }


    /**
     * Returns the valid key pair
     * @return
     */
    public KeyPair getKey(){

    	// load all the ingredients from shared preferences
    	String PUBLICModulusString = prefs.getString(PUBLIC_MODULUS, null);
    	String PUBLICExponentString = prefs.getString(PUBLIC_EXPONENT, null);
    	String PRIVATEModulusString = prefs.getString(PRIVATE_MODULUS, null);
    	String PRIVATEExponentString = prefs.getString(PRIVATE_EXPONENT, null);

    	//return generateKey();

    	// if we had a key saved, this should be true. otherwise we will now create one.
    	if(PUBLICModulusString != null && PUBLICExponentString != null && PRIVATEModulusString != null && PRIVATEExponentString != null){

    		try{

    			KeyFactory fact = KeyFactory.getInstance("RSA");
    			RSAPublicKeySpec pub = new RSAPublicKeySpec(new BigInteger(PUBLICModulusString), new BigInteger(PUBLICExponentString));
    			RSAPublicKey publicKey = (RSAPublicKey) fact.generatePublic(pub);

    			RSAPrivateKeySpec priv = new RSAPrivateKeySpec(new BigInteger(PRIVATEModulusString),new BigInteger(PRIVATEExponentString));
    			RSAPrivateKey privKey = (RSAPrivateKey) fact.generatePrivate(priv);

    			KeyPair kp = new KeyPair(publicKey, privKey);
    			return kp;

    		} catch(Exception e) {
    			Log.e(TAG, "Exception while getting keys!");
    		}
    	} else {
    		KeyPair kp = generateKey();
    		return kp;
    	}

    	return null;

    }

    /**
     * Creates PEM Format of the encoded (PUBLICCS #8?) public key
     * TODO: Let BouncyCastle (SpongyCastle) take care of this
     * @param kp
     * @return
     */
    public static String getPemPublicKey(KeyPair kp){
    	return getPemPublicKey(kp.getPublic());
    }

    public static String getPemPublicKey(PublicKey key){
    	String encoded = new String(Base64.encode(key.getEncoded(), Base64.DEFAULT));
        encoded = encoded.replace("\n", "");
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN PUBLIC KEY-----");
        builder.append("\n");
        int i = 0;
        while (i < encoded.length()) {
                builder.append(encoded.substring(i,
                                Math.min(i + 64, encoded.length())));
                builder.append("\n");
                i += 64;
        }
        builder.append("-----END PUBLIC KEY-----");

        return builder.toString();
}


    /**
     * Generate a new public/private key pair
     */
    public KeyPair generateKey(){

            try{

                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(Constants.SECURITY_KEY_SIZE);
                    KeyPair kp = kpg.genKeyPair();          
                    Log.i(TAG,"keys created");

                    // now we have to save the newly created keys!
                    if(saveKey(kp)){
                            return kp;
                    } else {
                            return null;
                    }

            }
            catch(Exception e){
                    Log.e(TAG , "Exception while generating keys!");
                    return null;
            }
    }

    /**
     * Save a newly generated Key pair
     */
    public boolean saveKey(KeyPair kp){

            try{
                    KeyFactory fact = KeyFactory.getInstance("RSA");
                    RSAPublicKeySpec pub = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
                    RSAPrivateKeySpec priv = fact.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);                    

                    SharedPreferences.Editor editor = prefs.edit();

                    // public key
                    editor.putString(PUBLIC_MODULUS, pub.getModulus().toString());
                    editor.putString(PUBLIC_EXPONENT, pub.getPublicExponent().toString());

                    // private key
                    editor.putString(PRIVATE_MODULUS, priv.getModulus().toString());
                    editor.putString(PRIVATE_EXPONENT, priv.getPrivateExponent().toString());

                    // finally, commit the changes to shared preferences
                    editor.commit();

                    Log.i(TAG, "keys saved");
                    return true;
            } catch(Exception e) {
                    Log.e(TAG, "Exception while saving keys!");
                    return false;
            }

    }

    /**
     * Deletes the current key from the shared preferences
     */
    public void deleteKey(){

            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(PUBLIC_MODULUS);
            editor.remove(PUBLIC_EXPONENT);
            editor.remove(PRIVATE_MODULUS);
            editor.remove(PRIVATE_EXPONENT);
            editor.commit();

    }

    /**
     * Parse key in PEM format
     */
    public static RSAPublicKey parsePem(String pemString){


            RSAPublicKey PUBLIC = null;

            PEMReader pem = new PEMReader(new StringReader(pemString));
            try {
                    PUBLIC = (RSAPublicKey) pem.readObject();
            } catch (IOException e) {
                    Log.e(TAG, "error reading key");
            }

            return PUBLIC;
    }
   
    private byte[] computeHash(String text) {
            MessageDigest crypt;
           
            try {
                    crypt = MessageDigest.getInstance("SHA-1");
                    crypt.reset();
                    crypt.update(text.getBytes());
                    return crypt.digest();
                   
            } catch (NoSuchAlgorithmException e) {
                   
            }
            return null;

    }

    /**
     * Computes a signature: RSA Encrypted the hash of the text.
     * @param text Text to sign
     * @return String the Base64 encoded string of the signature.
     */
    public String getSignature(String text){

    	byte[] hash = computeHash(text);      
    	//byte[] test = computeHash("paolo");    	
    	//Log.i(TAG, Base64.encodeToString(test, Base64.DEFAULT));
    	if (hash != null) {
    		try {                  
    			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    			KeyPair kp = getKey();                      

    			RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
    			//Log.i(TAG, "my public key: " + getPemPublicKey(kp.getPublic()));
    			cipher.init(Cipher.ENCRYPT_MODE, privateKey);
    			byte[] signature = cipher.doFinal(hash);                            
    			String signatureString = Base64.encodeToString(signature, Base64.DEFAULT);
    			Log.i(TAG, "Signature: " + signatureString);
    			
    			return signatureString;                

    		} catch (NoSuchAlgorithmException e) {                          
    		} catch (NoSuchPaddingException e) {                            
    		} catch (IllegalBlockSizeException e) {                        
    		} catch (BadPaddingException e) {                              
    		} catch (InvalidKeyException e) {                              
    		}
    	}
    	return null;
    }



    /**
     * Checks if a given signature matches the text, for the public key provided in the certificate object
     * @param cert
     * @param signature
     * @param text
     * @return
     */
    public boolean checkSignature(X509CertificateObject cert, String signature, String text) {


    	byte[] originalHash = computeHash(text);
    	

    	if (originalHash != null) {
    		try {

    			// get the public key from the certificate
    			RSAPublicKey publickey = (RSAPublicKey) cert.getPublicKey();
    			Log.d(TAG, "peer public key " + getPemPublicKey(publickey));
    			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");                                  
    			cipher.init(Cipher.DECRYPT_MODE, publickey);
    			// we get the signature in base 64 encoding -> decode first      		
    			return Arrays.equals(originalHash,cipher.doFinal(Base64.decode(signature, Base64.DEFAULT)) );                                  

    		} catch (NoSuchAlgorithmException e) {                  
    		} catch (NoSuchPaddingException e) {                    
    		} catch (InvalidKeyException e) {                      
    		} catch (IllegalBlockSizeException e) {                
    		} catch (BadPaddingException e) {                      
    		}
    	}


    	return false;
    }

    public String encrypt(String text, Long twitterId ) {          
    	try {                  
    		Cipher cipher = Cipher.getInstance("RSA");
    		//I NEED PEER'S PUBLIC KEY    
    		FriendsKeysDBHelper kHelper = new FriendsKeysDBHelper(context);
    		kHelper.open();
    		if (kHelper.hasKey(twitterId)) {
    			Log.i(TAG,"has Key");
    			String publicKeyString =  kHelper.getKey(twitterId);                                    
    			RSAPublicKey publicKey = parsePem(publicKeyString);
    			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    			byte[] cipherText = cipher.doFinal(text.getBytes());

    			String cipherTextString = Base64.encodeToString(cipherText, Base64.DEFAULT);                    

    			return cipherTextString;        
    		}


    	} catch (NoSuchAlgorithmException e) {
    		Log.e(TAG,"NoSuchAlgorithmException",e);
    	} catch (NoSuchPaddingException e) {
    		Log.e(TAG,"NoSuchPaddingException",e);
                    } catch (IllegalBlockSizeException e) {
                            Log.e(TAG,"IllegalBlockSizeException",e);
                    } catch (BadPaddingException e) {
                            Log.e(TAG,"error",e);
                    } catch (InvalidKeyException e) {      
                            Log.e(TAG,"error",e);
                    }
                    return null;

     }
     
      public String decrypt(String cipherData) {
             
              try {                  
                        KeyPair kp = getKey();                          

                            RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
                           
                            Cipher cipher = Cipher.getInstance("RSA");                                  
                            cipher.init(Cipher.DECRYPT_MODE, privateKey);
                            // we get the signature in base 64 encoding -> decode first
                            String decryptedText = new String(cipher.doFinal(Base64.decode(cipherData, Base64.DEFAULT)));
                            return decryptedText;
                           
                    } catch (NoSuchAlgorithmException e) {                  
                    } catch (NoSuchPaddingException e) {                    
                    } catch (InvalidKeyException e) {                      
                    } catch (IllegalBlockSizeException e) {                
                    } catch (BadPaddingException e) {                      
                    }
              return null;
               
      }

}

