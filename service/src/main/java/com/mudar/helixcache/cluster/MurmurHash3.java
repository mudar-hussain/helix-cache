package com.mudar.helixcache.cluster;

import java.nio.charset.StandardCharsets;

public class MurmurHash3 implements HashFunction {

    private static final long SEED = 0;

    @Override
    public Long hash(String value) {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);

        long h1 = SEED;
        long h2 = SEED;

        final int length = data.length;
        final int roundedEnd = (length & 0xffffffF0);

        for (int i = 0; i < roundedEnd; i += 16) {
            long k1 = getLong(data, i);
            long k2 = getLong(data, i + 8);

            k1 *= 0x87c37b91114253d5L;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= 0x4cf5ad432745937fL;

            h1 ^= k1;
            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            k2 *= 0x4cf5ad432745937fL;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= 0x87c37b91114253d5L;

            h2 ^= k2;
            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        long k1 = 0;
        long k2 = 0;

        switch (length & 15) {
            case 15:
                k2 ^= ((long) data[roundedEnd + 14] & 0xff) << 48;
            case 14:
                k2 ^= ((long) data[roundedEnd + 13] & 0xff) << 40;
            case 13:
                k2 ^= ((long) data[roundedEnd + 12] & 0xff) << 32;
            case 12:
                k2 ^= ((long) data[roundedEnd + 11] & 0xff) << 24;
            case 11:
                k2 ^= ((long) data[roundedEnd + 10] & 0xff) << 16;
            case 10:
                k2 ^= ((long) data[roundedEnd + 9] & 0xff) << 8;
            case 9:
                k2 ^= ((long) data[roundedEnd + 8] & 0xff);
                k2 *= 0x4cf5ad432745937fL;
                k2 = Long.rotateLeft(k2, 33);
                k2 *= 0x87c37b91114253d5L;
                h2 ^= k2;
            case 8:
                k1 ^= ((long) data[roundedEnd + 7] & 0xff) << 56;
            case 7:
                k1 ^= ((long) data[roundedEnd + 6] & 0xff) << 48;
            case 6:
                k1 ^= ((long) data[roundedEnd + 5] & 0xff) << 40;
            case 5:
                k1 ^= ((long) data[roundedEnd + 4] & 0xff) << 32;
            case 4:
                k1 ^= ((long) data[roundedEnd + 3] & 0xff) << 24;
            case 3:
                k1 ^= ((long) data[roundedEnd + 2] & 0xff) << 16;
            case 2:
                k1 ^= ((long) data[roundedEnd + 1] & 0xff) << 8;
            case 1:
                k1 ^= ((long) data[roundedEnd] & 0xff);
                k1 *= 0x87c37b91114253d5L;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= 0x4cf5ad432745937fL;
                h1 ^= k1;
        }

        h1 ^= length;
        h2 ^= length;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;

        return h1;
    }

    private long getLong(byte[] data, int index) {
        return ((long) data[index] & 0xff)
                | (((long) data[index + 1] & 0xff) << 8)
                | (((long) data[index + 2] & 0xff) << 16)
                | (((long) data[index + 3] & 0xff) << 24)
                | (((long) data[index + 4] & 0xff) << 32)
                | (((long) data[index + 5] & 0xff) << 40)
                | (((long) data[index + 6] & 0xff) << 48)
                | (((long) data[index + 7] & 0xff) << 56);
    }

    private long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }
}
