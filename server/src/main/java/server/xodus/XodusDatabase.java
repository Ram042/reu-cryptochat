package server.xodus;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import server.db.Database;
import server.db.MessageDatabase;
import server.db.SessionDatabase;
import server.db.UserDatabase;

public class XodusDatabase implements Database {

    private final Environment environment;
    private Store userStore;
    private Store messageStore;
    private Store sessionStore;

    public XodusDatabase() {
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

    @Override
    public MessageDatabase getMessageDatabase() {
        return new XodusMessageDatabase(messageStore);
    }

    @Override
    public UserDatabase getUserDatabase() {
        return new XodusUserDatabase(userStore);
    }

    @Override
    public SessionDatabase getSessionDatabase() {
        return new XodusSessionDatabase(sessionStore);
    }

    void clear() {
        environment.clear();
    }
}
