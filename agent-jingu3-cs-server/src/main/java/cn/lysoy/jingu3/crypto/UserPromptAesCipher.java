package cn.lysoy.jingu3.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 用户提示词落库：AES-256-GCM，单次随机 IV（12 字节），密文为 Base64( IV || ciphertext+tag )。
 */
public final class UserPromptAesCipher {

    private static final String AES_GCM = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_BITS = 128;

    private static final int AES_KEY_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private UserPromptAesCipher() {
    }

    /**
     * @throws IllegalArgumentException 长度非 32 字节
     */
    public static byte[] decodeKey256FromBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("AES key base64 empty");
        }
        byte[] key = Base64.getDecoder().decode(base64.trim());
        if (key.length != AES_KEY_BYTES) {
            throw new IllegalArgumentException("AES key must decode to 32 bytes, got " + key.length);
        }
        return key;
    }

    public static String encryptUtf8ToBase64(String plaintext, byte[] key256) throws GeneralSecurityException {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext null");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(AES_GCM);
        SecretKey secretKey = new SecretKeySpec(key256, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
        buf.put(iv);
        buf.put(ct);
        return Base64.getEncoder().encodeToString(buf.array());
    }
}
