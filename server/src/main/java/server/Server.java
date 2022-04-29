package server;

import io.javalin.Javalin;
import server.api.Message;
import server.api.Session;
import server.api.User;
import server.xodus.XodusDatabase;

public class Server {

    private final Javalin javalin;

    public Server() {
        javalin = Javalin.create();

        XodusDatabase database = new XodusDatabase();

        User user = new User(database.getUserDatabase());
        javalin.post("/user", user::create);
        javalin.get("/user", user::get);

        Session session = new Session(database.getSessionDatabase());
        javalin.post("/session", session::addInit);
        javalin.get("/session/{message}", session::getInit);

        Message message = new Message(database.getMessageDatabase());
        javalin.post("/message", message::add);
        javalin.get("/message/{message}", message::get);
    }

    public static void main(String[] args) {
        new Server().start();
    }


    public void start() {
        javalin.start(6060);
    }

    public void stop() {
        javalin.stop();
    }
}
