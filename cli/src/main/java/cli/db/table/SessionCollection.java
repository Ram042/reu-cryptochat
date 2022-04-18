package cli.db.table;

import cli.db.model.Session;
import moe.orangelabs.protoobj.Obj;
import moe.orangelabs.protoobj.types.ObjMap;
import org.dizitart.no2.Document;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.SortOrder;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.function.BiFunction;

import static com.google.common.io.BaseEncoding.base64;
import static org.dizitart.no2.FindOptions.sort;
import static org.dizitart.no2.UpdateOptions.updateOptions;
import static org.dizitart.no2.filters.Filters.and;
import static org.dizitart.no2.filters.Filters.eq;

public class SessionCollection {

    private static Session.SessionBuilder builder;
    private final NitriteCollection sessions;

    public SessionCollection(NitriteCollection sessions) {
        this.sessions = sessions;
    }

    public record SessionContainer(String target,
                                   Instant index,
                                   String data) {

    }

    /**
     * Latest session for target.
     * No additional checks
     */
    @Nullable
    public Session getLatestSession(byte[] target) {
        var targetEncoded = base64().encode(target);

        var doc = sessions.find(
                        eq("target", targetEncoded),
                        sort("instant", SortOrder.Descending))
                .firstOrDefault();
        if (doc == null) {
            return null;
        }

        var data = doc.get("data", String.class);

        return deserializeSession(data);
    }

    /**
     * Latest session for target with both parties ephemeral keys set
     */
    @Nullable
    public Session getLatestPreparedSession(byte[] target) {
        var targetEncoded = base64().encode(target);

        var doc = sessions.find(
                        and(
                                eq("target", targetEncoded),
                                eq("ephemeralSet", true),
                                eq("ephemeralTargetSet", true)
                        ),
                        sort("instant", SortOrder.Descending))
                .firstOrDefault();
        if (doc == null) {
            return null;
        }

        var data = doc.get("data", String.class);

        return deserializeSession(data);
    }

    /**
     * Get session for target and seed
     */
    public Session getSessionBySeed(byte[] target, byte[] seed) {
        var targetEncoded = base64().encode(target);
        var seedEncoded = base64().encode(seed);

        var doc = sessions.find(
                        and(
                                eq("target", targetEncoded),
                                eq("seed", seedEncoded)
                        ),
                        sort("instant", SortOrder.Descending))
                .firstOrDefault();

        if (doc == null) {
            return null;
        }

        var data = doc.get("data", String.class);

        return deserializeSession(data);
    }

    public void addSession(Session session) {
        var target = base64().encode(session.getTarget());
        var seed = base64().encode(session.getSeed());
        var instant = session.getInstant().toEpochMilli();

        var doc = Document.createDocument("target", target)
                .put("instant", instant)
                .put("seed", seed)
                .put("ephemeralSet", session.getEphemeralKey() != null)
                .put("ephemeralTargetSet", session.getTargetEphemeralPublicKey() != null)
                .put("data", serializeSession(session));

        sessions.update(and(eq("target", target), eq("seed", seed)), doc, updateOptions(true));
    }

    private static String serializeSession(Session session) {
        return base64().encode(Obj.map(
                "target", session.getTarget(),
                "seed", session.getSeed(),
                "ephemeralKey", session.getEphemeralKey(),
                "targetEphemeralPublicKey", session.getTargetEphemeralPublicKey(),
                "instant", session.getInstant().toEpochMilli()
        ).encode());
    }

    private static Session deserializeSession(String data) {
        ObjMap map = Obj.decode(base64().decode(data)).getAsMap();
        builder = Session.builder();

        BiFunction<ObjMap, String, byte[]> f = (m, s) -> {
            if (map.get(s).isNull()) {
                return null;
            } else {
                return map.getData(s).getData();
            }
        };

        builder.target(f.apply(map, "target"));
        builder.seed(f.apply(map, "seed"));
        builder.ephemeralKey(f.apply(map, "ephemeralKey"));
        builder.targetEphemeralPublicKey(f.apply(map, "targetEphemeralPublicKey"));

        builder.instant(Instant.ofEpochMilli(map.getInteger("instant").longValue()));

        return builder.build();
    }

}
