package com.kbank.core.utils.impl;

import com.kbank.core.utils.KbankEncryptData;
import org.osgi.service.component.annotations.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component(service = KbankEncryptData.class, immediate = true)
public class KbankEncryptDataImpl implements KbankEncryptData{

    private static final String SECRET_KEY = "MySecretKey12345";

    @Override
    public String encryptData(String data) throws IllegalBlockSizeException, BadPaddingException,
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        // Create secret key
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");

        // Generate nonce
        byte[] nonce = new byte[12];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Combine nonce and encrypted data
        byte[] combined = new byte[nonce.length + encryptedBytes.length];
        System.arraycopy(nonce, 0, combined, 0, nonce.length);
        System.arraycopy(encryptedBytes, 0, combined, nonce.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    @Override
    public String decryptData(String data) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        // Decode base64
        byte[] combined = Base64.getDecoder().decode(data);

        // Extract nonce and encrypted data
        byte[] nonce = new byte[12];
        byte[] encryptedBytes = new byte[combined.length - 12];
        System.arraycopy(combined, 0, nonce, 0, nonce.length);
        System.arraycopy(combined, nonce.length, encryptedBytes, 0, encryptedBytes.length);

        // Create secret key and GCM spec
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce);

        // Initialize cipher and decrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
