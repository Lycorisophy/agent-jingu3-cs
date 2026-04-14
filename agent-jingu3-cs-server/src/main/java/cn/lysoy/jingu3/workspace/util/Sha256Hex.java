package cn.lysoy.jingu3.workspace.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * UTF-8 文本的 SHA-256 十六进制摘要（执行历史 code_hash 等）。
 */
public final class Sha256Hex {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Sha256Hex() {
    }

    public static String ofUtf8(String text) {
        if (text == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                int v = b & 0xff;
                sb.append(HEX[v >>> 4]).append(HEX[v & 0x0f]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256", e);
        }
    }
}
