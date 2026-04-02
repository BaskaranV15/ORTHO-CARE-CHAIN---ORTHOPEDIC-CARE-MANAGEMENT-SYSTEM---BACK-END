package com.orthocarechain.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM JPA AttributeConverter.
 *
 * Transparently encrypts String fields before writing to the database
 * and decrypts them after reading — no service or controller code changes needed.
 *
 * Storage format in DB column:
 *   Base64( IV(12 bytes) + Ciphertext + GCM AuthTag(16 bytes) )
 */
@Converter
@Component
public class AesEncryptConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH  = 12;   // 96-bit IV — GCM standard
    private static final int    TAG_LENGTH = 128;  // 128-bit auth tag — GCM standard
    private static final int    KEY_BITS   = 256;

    // Injected from application.properties / environment variable
    // Must be exactly 32 characters (256 bits)
    @Value("${aes.secret.key}")
    private String secretKeyString;

    // ── Encrypt: called before saving to DB ───────────────────────────────────
    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;

        try {
            SecretKey key = buildKey();

            // Fresh random IV for every encryption — critical for GCM security
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Prepend IV to ciphertext so we can extract it on decrypt
            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            throw new RuntimeException("AES-256-GCM encryption failed", e);
        }
    }

    // ── Decrypt: called after reading from DB ─────────────────────────────────
    @Override
    public String convertToEntityAttribute(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) return encryptedBase64;

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            // Extract the IV from the first 12 bytes
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv         = new byte[IV_LENGTH];
            byte[] cipherText = new byte[decoded.length - IV_LENGTH];
            buffer.get(iv);
            buffer.get(cipherText);

            SecretKey key    = buildKey();
            Cipher    cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText), "UTF-8");

        } catch (Exception e) {
            throw new RuntimeException("AES-256-GCM decryption failed", e);
        }
    }

    // ── Build SecretKey from the configured string ─────────────────────────────
    private SecretKey buildKey() {
        byte[] keyBytes = secretKeyString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "AES-256 key must be exactly 32 characters. Current length: " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
