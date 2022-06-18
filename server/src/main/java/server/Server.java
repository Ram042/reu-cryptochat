package server;

import io.javalin.Javalin;
import server.api.MessageApi;
import server.api.SessionApi;
import server.api.UserApi;
import server.xodus.Database;

public class Server {

    private final Javalin javalin;

    public Server() {
        javalin = Javalin.create();

        Database database = new Database();

        UserApi user = new UserApi(database.getUserDatabase());
        javalin.post("/user", user::create);
        javalin.get("/user", user::get);

        SessionApi session = new SessionApi(database.getSessionDatabase());
        javalin.post("/session", session::addInit);
        javalin.get("/session/{message}", session::getInit);

        MessageApi message = new MessageApi(database.getMessageDatabase());
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
