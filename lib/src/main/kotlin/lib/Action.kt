package lib

enum class Action {
    /**
     * Register user on server. Use to save user info on server
     */
    USER_REGISTER,

    /**
     * Send to another party public key of session
     */
    SESSION_UPDATE,

    /**
     * Request session updates from server
     */
    SESSION_GET,

    /**
     * Send an encrypted message
     */
    ENVELOPE,

    /**
     * Receive encrypted messages from server
     */
    ENVELOPE_GET
}
