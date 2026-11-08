package io.github.vibernoslop;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Minimal read-only DEX string-reference index. No loading of unrelated app classes. */
final class DexFinder {
    static final String EXPERIMENT_ANCHOR = "Too much time spent on mapping experiment=";
    static Set<String> find(String apk, String anchor) throws Exception {
        return findAll(apk, new String[]{anchor}).get(anchor);
    }
    static java.util.Map<String, Set<String>> findAll(String apk, String[] anchors) throws Exception {
        java.util.Map<String, Set<String>> result = new java.util.LinkedHashMap<>();
        for (String anchor : anchors) result.put(anchor, new LinkedHashSet<>());
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.getName().matches("classes[0-9]*\\.dex")) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[32768];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    Index index = new Index(out.toByteArray());
                    for (String anchor : anchors) index.find(anchor, result.get(anchor));
                }
            }
        }
        return result;
    }
    private static final class Index {
        final byte[] data;
        Index(byte[] data) {
            this.data = data;
            if (data.length < 112 || data[0] != 'd' || data[1] != 'e' || data[2] != 'x')
                throw new IllegalArgumentException("Unsupported DEX header");
        }
        int u16(int p) { return (data[p] & 255) | ((data[p + 1] & 255) << 8); }
        int i32(int p) { return u16(p) | (u16(p + 2) << 16); }
        int leb(int[] p) {
            int value = 0;
            for (int shift = 0; shift < 35; shift += 7) {
                int b = data[p[0]++] & 255;
                value |= (b & 127) << shift;
                if ((b & 128) == 0) return value;
            }
            throw new IllegalArgumentException("Invalid ULEB128");
        }
        String string(int index) {
            int[] p = {i32(i32(60) + 4 * index)};
            leb(p);
            int end = p[0];
            while (data[end] != 0) end++;
            // Anchors and descriptors are ASCII; no MUTF-8 conversion is needed for them.
            return new String(data, p[0], end - p[0], StandardCharsets.UTF_8);
        }
        void find(String anchor, Set<String> found) {
            int target = -1;
            byte[] expected = anchor.getBytes(StandardCharsets.US_ASCII);
            for (int i = 0, count = i32(56); i < count; i++) {
                int[] offset = {i32(i32(60) + 4 * i)};
                if (leb(offset) != expected.length) continue;
                int j = 0;
                while (j < expected.length && data[offset[0] + j] == expected[j]) j++;
                if (j == expected.length && data[offset[0] + j] == 0) { target = i; break; }
            }
            if (target < 0) return;
            int classes = i32(100);
            for (int i = 0, count = i32(96); i < count; i++) {
                int cls = classes + i * 32;
                int off = i32(cls + 24);
                if (off == 0) continue;
                int[] p = {off};
                int sf = leb(p), inf = leb(p), dm = leb(p), vm = leb(p);
                for (int f = 0; f < sf + inf; f++) { leb(p); leb(p); }
                boolean match = false;
                for (int m = 0; m < dm + vm; m++) {
                    leb(p); leb(p);
                    int code = leb(p);
                    if (code != 0 && references(code, target)) match = true;
                }
                if (match) {
                    int type = i32(cls);
                    String descriptor = string(i32(i32(68) + type * 4));
                    found.add(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
                }
            }
        }
        boolean references(int code, int stringId) {
            int start = code + 16, size = i32(code + 12);
            for (int unit = 0; unit < size;) {
                int at = start + unit * 2, ins = u16(at), op = ins & 255;
                if (op == 0x1a && unit + 1 < size && u16(at + 2) == stringId) return true;
                if (op == 0x1b && unit + 2 < size && i32(at + 2) == stringId) return true;
                int width = width(op);
                if (op == 0 && ins != 0) {
                    switch (ins) {
                        case 0x0100: width = 4 + u16(at + 2) * 2; break;
                        case 0x0200: width = 2 + u16(at + 2) * 4; break;
                        case 0x0300: width = 4 + (int) (((long) u16(at + 2) * i32(at + 4) + 1) / 2); break;
                        default: throw new IllegalArgumentException("Unknown DEX payload");
                    }
                }
                if (width <= 0 || width > size - unit) throw new IllegalArgumentException("Invalid DEX instruction");
                unit += width;
            }
            return false;
        }
        static int width(int op) {
            if (op == 0x18) return 5;
            if (op == 0xfa || op == 0xfb) return 4;
            if (op == 0x03 || op == 0x06 || op == 0x09 || op == 0x14 || op == 0x17 || op == 0x1b
                || (op >= 0x24 && op <= 0x26) || (op >= 0x2a && op <= 0x2c)
                || (op >= 0x6e && op <= 0x72) || (op >= 0x74 && op <= 0x78)
                || op == 0xfc || op == 0xfd) return 3;
            if (op == 0x02 || op == 0x05 || op == 0x08 || op == 0x13 || op == 0x15 || op == 0x16
                || op == 0x19 || op == 0x1a || op == 0x1c || op == 0x1f || op == 0x20
                || op == 0x22 || op == 0x23 || op == 0x29 || (op >= 0x2d && op <= 0x3d)
                || (op >= 0x44 && op <= 0x6d) || (op >= 0x90 && op <= 0xaf)
                || (op >= 0xd0 && op <= 0xe2) || op == 0xfe || op == 0xff) return 2;
            return 1;
        }
    }
}
