package lib;

import moe.orangelabs.protoobj.Obj;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

public final class Signer {

    private Signer() {
    }

    public static byte[] sign(byte[] privateKey, byte[] data) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, new Ed25519PrivateKeyParameters(privateKey));
        signer.update(data, 0, data.length);
        return signer.generateSignature();
    }

    public static byte[] getPublicKeyForPrivate(byte[] privateKey) {
        return new Ed25519PrivateKeyParameters(privateKey).generatePublicKey().getEncoded();
    }

    public static Obj generateSignedPackage(byte[] privateKey, byte[] data) {
        return Obj.map("PUBLIC_KEY", getPublicKeyForPrivate(privateKey),
                "SIG", sign(privateKey, data),
                "DATA", data);
    }

    public static boolean verify(byte[] publicKey, byte[] signature, byte[] data) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, new Ed25519PublicKeyParameters(publicKey));
        signer.update(data, 0, data.length);
        return signer.verifySignature(signature);
    }

}
