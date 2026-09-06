package com.mudar.helixcache.cluster;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Sha256HashFunction implements HashFunction {

    private static final ThreadLocal<MessageDigest> DIGEST =
            ThreadLocal.withInitial(() -> {
                try {
                    return MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("SHA-256 not available", e);
                }
            });

    @Override
    public Long hash(String value) {

        MessageDigest digest = DIGEST.get();
        digest.reset();

        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

        // Use the first 8 bytes as an unsigned 64-bit ring position
        return ByteBuffer.wrap(hash).getLong() & Long.MAX_VALUE;
    }
}
