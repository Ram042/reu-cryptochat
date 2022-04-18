package lib.utils;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

public class Base62Test {

    @Test
    public void testConversion() {
        assertThat(Base62.encode(new byte[]{29, 121, (byte) 247})).isEqualTo("86XX");

        assertThat(Base62.decodeString("2pM")).isEqualTo(new byte[]{42, 120});
    }
}
