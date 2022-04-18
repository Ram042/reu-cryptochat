package server.db;

public interface Database {

    MessageDatabase getMessageDatabase();

    UserDatabase getUserDatabase();

    SessionDatabase getSessionDatabase();
}
