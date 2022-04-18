package server.db;

public interface UserDatabase {

    void addUser(byte[] id, byte[] profile);

    byte[] getUser(byte[] id);

    /**
     *
     * @param id
     * @return true if user was successfully removed
     */
    boolean removeUser(byte[] id);

}
