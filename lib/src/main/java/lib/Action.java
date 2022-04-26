package lib;

public enum Action {

    /**
     * Register user on server. Use to save user info on server
     */
    USER_REGISTER,
    /**
     * Send to other party public key of session
     */
    SESSION_UPDATE,
    /**
     * Request session updates from server
     */
    SESSION_GET,
    /**
     * Send encrypted message
     */
    ENVELOPE,
    /**
     * Receive encrypted messages from server
     */
    ENVELOPE_GET

}
