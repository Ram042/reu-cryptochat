package cli.db.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session {

    /**
     * Public key of target
     */
    @NotNull
    byte[] target;
    /**
     * Seed
     */
    @NotNull
    byte[] seed;
    /**
     * Our ephemeral private key
     */
    @Nullable
    byte[] ephemeralKey;
    /**
     * Target ephemeral public key
     */
    @Nullable
    byte[] targetEphemeralPublicKey;
    /**
     * When session was generated
     */
    @NotNull
    Instant instant;
}
