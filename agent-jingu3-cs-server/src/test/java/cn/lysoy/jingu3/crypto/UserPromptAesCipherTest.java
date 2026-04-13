package cn.lysoy.jingu3.crypto;

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPromptAesCipherTest {

    @Test
    void encrypt_roundTripDistinctIv() throws GeneralSecurityException {
        byte[] rawKey = new byte[32];
        Arrays.fill(rawKey, (byte) 7);
        String keyB64 = Base64.getEncoder().encodeToString(rawKey);
        byte[] key = UserPromptAesCipher.decodeKey256FromBase64(keyB64);
        String c1 = UserPromptAesCipher.encryptUtf8ToBase64("hello", key);
        String c2 = UserPromptAesCipher.encryptUtf8ToBase64("hello", key);
        assertThat(c1).isNotBlank().isNotEqualTo(c2);
    }

    @Test
    void badKeyLength_rejected() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> UserPromptAesCipher.decodeKey256FromBase64(shortKey))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
