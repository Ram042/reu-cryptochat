package server.xodus;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;

public class Database {

    private final Environment environment;
    private Store userStore;
    private Store messageStore;
    private Store sessionStore;

    public Database() {
        environment = Environments.newInstance("cryptochat");

        initStores();
    }

    void initStores() {
        userStore = environment.computeInTransaction(txn ->
                environment.openStore("users", StoreConfig.WITHOUT_DUPLICATES, txn));
        sessionStore = environment.computeInTransaction(txn ->
                environment.openStore("session", StoreConfig.WITH_DUPLICATES, txn));
        messageStore = environment.computeInTransaction(txn ->
                environment.openStore("messages", StoreConfig.WITH_DUPLICATES, txn));
    }

    public MessageDatabase getMessageDatabase() {
        return new MessageDatabase(messageStore);
    }

    public UserDatabase getUserDatabase() {
        return new UserDatabase(userStore);
    }

    public SessionDatabase getSessionDatabase() {
        return new SessionDatabase(sessionStore);
    }

    void clear() {
        environment.clear();
    }
}
