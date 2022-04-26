package lib.utils;

import com.google.common.io.BaseEncoding;

public final class Base16 {

    private Base16() {
    }

    public static String encode(byte[] bytes) {
        return BaseEncoding.base16().lowerCase().encode(bytes);
    }

    public static byte[] decode(String string) {
        return BaseEncoding.base16().lowerCase().decode(string);
    }

}
