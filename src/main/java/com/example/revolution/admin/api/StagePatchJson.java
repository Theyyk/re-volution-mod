package com.example.revolution.admin.api;

import com.example.revolution.admin.definition.StagePatch;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict streaming parser: unknown/duplicate fields, duplicate stage rows and excessive batches are rejected. */
final class StagePatchJson {
    static List<StagePatch> parse(String input) {
        if (input == null || input.length() > 48000) throw new IllegalArgumentException("Patch body too large");
        try (JsonReader reader = new JsonReader(new StringReader(input))) {
            reader.setLenient(false);
            List<StagePatch> patches = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                if (patches.size() >= 100) throw new IllegalArgumentException("Maximum 100 stages per batch");
                reader.beginObject();
                Integer location = null, stage = null, population = null;
                String hp = null, resource = null, amount = null;
                Set<String> fields = new HashSet<>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!fields.add(name)) throw new IllegalArgumentException("Duplicate patch field");
                    switch (name) {
                        case "location": location = integer(reader); break;
                        case "stage": stage = integer(reader); break;
                        case "population": population = integer(reader); break;
                        case "hp": hp = text(reader); break;
                        case "resource": resource = text(reader); break;
                        case "amount": amount = text(reader); break;
                        default: throw new IllegalArgumentException("Unknown patch field");
                    }
                }
                reader.endObject();
                if (location == null || stage == null) throw new IllegalArgumentException("Stage identity missing");
                patches.add(new StagePatch(location, stage, hp, population, resource, amount));
            }
            reader.endArray();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing patch data");
            return patches;
        } catch (IOException | IllegalStateException e) { throw new IllegalArgumentException("Invalid patch JSON", e); }
    }
    private static int integer(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.NUMBER) throw new IllegalArgumentException("Expected integer");
        String raw = reader.nextString();
        if (!raw.matches("[0-9]{1,8}")) throw new IllegalArgumentException("Invalid integer");
        return Integer.parseInt(raw);
    }
    private static String text(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.STRING) throw new IllegalArgumentException("Exact numbers/resources must be strings");
        String value = reader.nextString();
        if (value.length() > 128) throw new IllegalArgumentException("Patch value too long");
        return value;
    }
}
