package cli.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;

@Data
@NoArgsConstructor
public class User {

    private byte[] key;

    private boolean encrypted;
    private byte[] nonce;
    private int counter;

    public static User generate() {
        var u = new User();

        var key = new byte[32];
        new SecureRandom().nextBytes(key);
        u.setKey(key);

        return u;
    }
}
