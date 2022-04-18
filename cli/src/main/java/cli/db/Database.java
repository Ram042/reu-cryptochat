package cli.db;

import cli.db.model.Message;
import cli.db.model.User;
import cli.db.table.SessionCollection;
import lombok.Getter;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;

public class Database implements Closeable {
    public static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    Nitrite db;
    @Getter
    private final SessionCollection sessionCollection;
    @Getter
    private final ObjectRepository<Message> messages;

    User user;

    public Database(String dbFile, String password, boolean create) {
        if (create) {
            if (Files.exists(Path.of(dbFile))) {

                LOGGER.warn("Database already exists");
                throw new RuntimeException("Database already exists");
            } else {
                db = Nitrite.builder()
                        .filePath(dbFile)
                        .openOrCreate();

                LOGGER.info("Generating key");
                user = User.generate();
                db.getRepository("user", User.class).insert(user);
            }
        } else {
            db = Nitrite.builder()
                    .filePath(dbFile)
                    .openOrCreate();
            user = db.getRepository("user", User.class).find().firstOrDefault();
        }

        sessionCollection = new SessionCollection(db.getCollection("sessions"));
        messages = db.getRepository("messages", Message.class);
    }


    public byte[] getKey() {
        return user.getKey().clone();
    }

    public void close() {
        db.close();
    }

}
