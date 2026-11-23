package com.mishcon.portal.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/** The hash rule for the audit log. Kept in one place so writing and verifying cannot drift apart. */
final class HashChain {
    static final String GENESIS = "0".repeat(64);

    private HashChain() {}

    static String hash(String prevHash, Instant time, String actor, String action, String requestId, String message) {
        String data = String.join("|", prevHash, time.toString(), actor, action, requestId, message);
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
