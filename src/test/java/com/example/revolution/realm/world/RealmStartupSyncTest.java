package com.example.revolution.realm.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/** Real filesystem contracts, without opening Minecraft worlds or MongoDB. */
public final class RealmStartupSyncTest {
    public static void main(String[] args) throws Exception {
        Path sandbox = Files.createTempDirectory("realm-startup-sync-");
        try {
            restartReplacesEntireSaves(sandbox.resolve("custom-level"));
            firstBoot(sandbox.resolve("fresh"));
            missingTemplatePreservesTargets(sandbox.resolve("missing"));
            interruptedSyncBlocksStartup(sandbox.resolve("interrupted"));
            invalidDestinationPreservesTargets(sandbox.resolve("invalid"));
            stagingFailurePreservesTargets(sandbox.resolve("staging-failure"));
            linksAreRejected(sandbox.resolve("links"), sandbox.resolve("outside"));
        } finally {
            try (Stream<Path> paths = Files.walk(sandbox)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
            }
        }
        System.out.println("Realm startup sync: full replacement, restart isolation, lock, bootstrap and failure guards passed");
    }

    private static void restartReplacesEntireSaves(Path root) throws Exception {
        write(root.resolve("level.dat"), "overworld metadata");
        write(root.resolve("region/r.0.0.mca"), "overworld blocks");
        write(root.resolve("playerdata/player.dat"), "shared player data");
        write(root.resolve("DIM-1/region/r.0.0.mca"), "nether");
        write(root.resolve("DIM201/region/r.0.0.mca"), "template blocks/entities/ticks");
        write(root.resolve("DIM201/data/arbitrary.dat"), "dimension capabilities");
        write(root.resolve("DIM201/data/villages_revolution_realm_1.dat"), "template villages");
        Files.createDirectories(root.resolve("DIM201/empty/nested"));
        write(root.resolve("DIM202/region/stale.mca"), "old player construction");
        write(root.resolve("DIM203/deep/stale.dat"), "old dimension data");
        try (RealmStartupSync owner = RealmStartupSync.prepare(root)) {
            for (int id = 202; id <= 203; id++) {
                checkBytes(root.resolve("DIM201/region/r.0.0.mca"), root.resolve("DIM" + id + "/region/r.0.0.mca"));
                checkBytes(root.resolve("DIM201/data/arbitrary.dat"), root.resolve("DIM" + id + "/data/arbitrary.dat"));
                check(Files.isDirectory(root.resolve("DIM" + id + "/empty/nested")), "copy empty directories");
                check(read(root.resolve("DIM" + id + "/data/villages_revolution_realm_" + (id - 200) + ".dat")).equals("template villages"), "village storage remapped");
            }
            check(!Files.exists(root.resolve("DIM202/region/stale.mca")), "remove target-only regions");
            check(!Files.exists(root.resolve("DIM203/deep")), "remove target-only directories");
            expectFailure(root);
            write(root.resolve("DIM202/region/r.0.0.mca"), "independent runtime changes");
            check(read(root.resolve("DIM201/region/r.0.0.mca")).equals("template blocks/entities/ticks"), "copies are independent files");
            check(read(root.resolve("DIM203/region/r.0.0.mca")).equals("template blocks/entities/ticks"), "other realm remains independent");
        }
        write(root.resolve("DIM201/region/r.0.0.mca"), "latest saved template");
        write(root.resolve("DIM203/new-player-file.dat"), "discard on restart");
        try (RealmStartupSync owner = RealmStartupSync.prepare(root)) {
            for (int id = 202; id <= 203; id++) check(read(root.resolve("DIM" + id + "/region/r.0.0.mca")).equals("latest saved template"), "next restart uses latest source");
        }
        check(!Files.exists(root.resolve("DIM203/new-player-file.dat")), "next restart resets player changes");
        check(read(root.resolve("level.dat")).equals("overworld metadata"), "Realm 0 metadata untouched");
        check(read(root.resolve("region/r.0.0.mca")).equals("overworld blocks"), "Realm 0 blocks untouched");
        check(read(root.resolve("playerdata/player.dat")).equals("shared player data"), "player data untouched");
        check(read(root.resolve("DIM-1/region/r.0.0.mca")).equals("nether"), "other dimensions untouched");
    }

    private static void firstBoot(Path root) throws Exception {
        try (RealmStartupSync owner = RealmStartupSync.prepare(root)) {
            for (int id = 201; id <= 203; id++) check(Files.isDirectory(root.resolve("DIM" + id)), "fresh empty Realm save");
        }
    }

    private static void missingTemplatePreservesTargets(Path root) throws Exception {
        write(root.resolve("DIM202/keep.dat"), "keep");
        expectFailure(root);
        check(read(root.resolve("DIM202/keep.dat")).equals("keep"), "missing source cannot erase a destination");
        write(root.resolve("DIM201/template.dat"), "template");
        try (RealmStartupSync owner = RealmStartupSync.prepare(root)) {
            check(read(root.resolve("DIM202/template.dat")).equals("template"), "failed prepare releases save lock");
        }
    }

    private static void interruptedSyncBlocksStartup(Path root) throws Exception {
        write(root.resolve("DIM201/template.dat"), "template");
        write(root.resolve("DIM202/keep.dat"), "keep");
        write(root.resolve(".revolution-realm-sync/old-DIM203/backup.dat"), "recovery");
        expectFailure(root);
        check(read(root.resolve("DIM202/keep.dat")).equals("keep"), "interrupted sync preserves current saves");
        check(read(root.resolve(".revolution-realm-sync/old-DIM203/backup.dat")).equals("recovery"), "interrupted sync preserves backup");
    }

    private static void invalidDestinationPreservesTargets(Path root) throws Exception {
        write(root.resolve("DIM201/template.dat"), "template");
        write(root.resolve("DIM202/keep.dat"), "keep");
        write(root.resolve("DIM203"), "invalid directory");
        expectFailure(root);
        check(read(root.resolve("DIM202/keep.dat")).equals("keep"), "validate both destinations before replacing either");
    }

    private static void stagingFailurePreservesTargets(Path root) throws Exception {
        // A filename collision fails after copying has begun, before either destination is published.
        write(root.resolve("DIM201/data/villages_revolution_realm_1.dat"), "source");
        write(root.resolve("DIM201/data/villages_revolution_realm_2.dat"), "collision");
        write(root.resolve("DIM202/keep.dat"), "second");
        write(root.resolve("DIM203/keep.dat"), "third");
        expectFailure(root);
        check(read(root.resolve("DIM202/keep.dat")).equals("second"), "staging failure preserves Realm 2");
        check(read(root.resolve("DIM203/keep.dat")).equals("third"), "staging failure preserves Realm 3");
        check(!Files.exists(root.resolve(".revolution-realm-sync")), "handled staging failure removes incomplete copies");
    }

    private static void linksAreRejected(Path root, Path outside) throws Exception {
        write(root.resolve("DIM201/template.dat"), "template");
        write(root.resolve("DIM202/keep.dat"), "keep");
        write(outside.resolve("keep.dat"), "outside");
        Path link = root.resolve("DIM201/linked");
        try { Files.createSymbolicLink(link, outside); }
        catch (UnsupportedOperationException | IOException e) {
            System.out.println("Symlink test skipped: host does not permit symlink creation (CI Linux runs it).");
            return;
        }
        expectFailure(root);
        check(read(root.resolve("DIM202/keep.dat")).equals("keep"), "source link cannot trigger replacement");
        check(read(outside.resolve("keep.dat")).equals("outside"), "link target remains untouched");
        Files.delete(link);
        Files.createSymbolicLink(root.resolve("DIM203"), outside);
        expectFailure(root);
        check(read(root.resolve("DIM202/keep.dat")).equals("keep"), "destination link cannot trigger replacement");
    }

    private static void expectFailure(Path root) throws Exception {
        try (RealmStartupSync ignored = RealmStartupSync.prepare(root)) {
            throw new AssertionError("Unsafe startup was accepted: " + root);
        } catch (IOException expected) { /* fail closed */ }
    }

    private static void write(Path file, String value) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, value.getBytes(StandardCharsets.UTF_8));
    }
    private static String read(Path file) throws IOException { return new String(Files.readAllBytes(file), StandardCharsets.UTF_8); }
    private static void checkBytes(Path first, Path second) throws IOException { check(Arrays.equals(Files.readAllBytes(first), Files.readAllBytes(second)), "exact file contents"); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
