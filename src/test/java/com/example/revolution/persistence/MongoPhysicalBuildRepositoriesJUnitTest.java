package com.example.revolution.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.generation.LocationGenerationPlanner;
import com.example.revolution.location.generation.LocationTemplate;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;
import org.junit.jupiter.api.Test;

final class MongoPhysicalBuildRepositoriesJUnitTest {
    @Test void persistsLocationBindingAndExactGenerationProvenanceWithoutWorldOrMongo() throws Exception {
        MongoConnection connection = new MongoConnection(LogManager.getLogger("physical-build-test"));
        AtomicReference<String> collection = new AtomicReference<>();
        AtomicReference<Document> filter = new AtomicReference<>();
        AtomicReference<Document> values = new AtomicReference<>();
        AtomicReference<Document> current = new AtomicReference<>();
        Field database = MongoConnection.class.getDeclaredField("database");
        database.setAccessible(true);
        database.set(connection, proxy(MongoDatabase.class, (object, method, args) -> {
            if (method.getName().equals("runCommand")) return new Document("ok", 1);
            if (!method.getName().equals("getCollection")) throw new AssertionError(method);
            collection.set((String) args[0]);
            return proxy(MongoCollection.class, (target, operation, arguments) -> {
                filter.set((Document) arguments[0]);
                if (operation.getName().equals("updateOne")) {
                    values.set(((Document) arguments[1]).get("$set", Document.class));
                    return null;
                }
                if (operation.getName().equals("deleteOne")) return null;
                if (operation.getName().equals("find")) return proxy(FindIterable.class, (iterator, query, ignored) -> {
                    if (query.getName().equals("first")) return current.get();
                    throw new AssertionError(query);
                });
                throw new AssertionError(operation);
            });
        }));
        MongoLocationBuildTemplateRepository bindings = new MongoLocationBuildTemplateRepository(connection, true);
        bindings.upsert(7, "sunny_thicket_71_v9_tapered_trunks");
        assertEquals("location_build_templates", collection.get());
        assertEquals(new Document("worldLocationId", 7), filter.get());
        current.set(values.get());
        assertEquals("sunny_thicket_71_v9_tapered_trunks", bindings.get(7));
        assertEquals(7, values.get().getInteger("worldLocationId"));

        LocationGenerationPlan plan = new LocationGenerationPlanner().plan(
                new LocationTemplate("sunny_thicket_71_v9_tapered_trunks", 30, Collections.emptyList()),
                7, 3, -100, 64, 20, Long.MIN_VALUE);
        MongoLocationGenerationRepository generations = new MongoLocationGenerationRepository(connection, true);
        generations.save(plan);
        assertEquals("location_generations", collection.get());
        assertEquals(new Document("locationId", plan.getInternalLocationId()), filter.get());
        assertEquals(Long.MIN_VALUE, values.get().getLong("seed"));
        assertEquals(plan.getRotationDegrees(), values.get().getInteger("rotationDegrees"));
        assertEquals(plan.getTemplateId(), values.get().getString("templateName"));
        assertEquals(-100D, values.get().getDouble("anchorX"));
        assertEquals(64D, values.get().getDouble("anchorY"));
        assertEquals(20D, values.get().getDouble("anchorZ"));
        generations.delete(plan.getInternalLocationId());
        assertEquals(new Document("locationId", plan.getInternalLocationId()), filter.get());
    }

    @Test void disabledOrDisconnectedStorageFailsClosed() {
        MongoConnection connection = new MongoConnection(LogManager.getLogger("physical-build-test"));
        assertThrows(IllegalStateException.class, () -> new MongoLocationBuildTemplateRepository(connection, false).upsert(1, "test"));
        assertThrows(IllegalStateException.class, () -> new MongoLocationBuildTemplateRepository(connection, true).get(1));
        assertThrows(IllegalStateException.class, () -> new MongoLocationGenerationRepository(connection, false).requireAvailable());
        assertThrows(IllegalStateException.class, () -> new MongoLocationGenerationRepository(connection, true).requireAvailable());
    }

    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }
}
