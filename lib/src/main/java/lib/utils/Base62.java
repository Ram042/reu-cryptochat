package lib.utils;


import java.nio.charset.StandardCharsets;

public class Base62 {

    private static io.seruco.encoding.base62.Base62 instance = io.seruco.encoding.base62.Base62.createInstance();

    private Base62() {
    }

    public static byte[] decode(String string) {
        instance = io.seruco.encoding.base62.Base62.createInstance();
        return instance.decode(string.getBytes(StandardCharsets.US_ASCII));
    }

    public static String encode(byte[] bytes) {
        return new String(instance.encode(bytes), StandardCharsets.US_ASCII);
    }
}
