package com.example.revolution.realm.world;

import com.example.revolution.realm.definition.RealmCatalog;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/** Offline dimension-save adapter. Only the dedicated pre-world-load lifecycle may call prepare. */
public final class RealmStartupSync implements AutoCloseable {
    private static final LinkOption NO_LINKS = LinkOption.NOFOLLOW_LINKS;
    private final FileChannel channel;
    private final FileLock lock;

    private RealmStartupSync(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** The returned lock must remain held until the server has closed its worlds. */
    public static RealmStartupSync prepare(Path saveRoot) throws IOException {
        Path root = saveRoot.toAbsolutePath().normalize();
        for (Path parent = root; parent != null; parent = parent.getParent()) rejectLink(parent);
        Files.createDirectories(root);
        Path lockPath = root.resolve(".revolution-realm.lock");
        rejectLink(lockPath);
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, NO_LINKS);
        RealmStartupSync owner = null;
        try {
            FileLock lock;
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException e) { throw new IOException("Realm save is already in use: " + root, e); }
            if (lock == null) throw new IOException("Realm save is already in use: " + root);
            owner = new RealmStartupSync(channel, lock);
            owner.synchronize(root);
            return owner;
        } catch (IOException | RuntimeException e) {
            try {
                if (owner != null) owner.close();
                else channel.close();
            } catch (IOException closeFailure) { e.addSuppressed(closeFailure); }
            throw e;
        }
    }

    private void synchronize(Path root) throws IOException {
        Path source = dimension(root, 1);
        Path[] targets = {dimension(root, 2), dimension(root, 3)};
        Path transaction = root.resolve(".revolution-realm-sync");
        if (Files.exists(transaction, NO_LINKS)) {
            throw new IOException("Interrupted Realm sync: inspect and recover " + transaction + " before starting");
        }
        // Validate every input before replacing either destination. Never follow links/junctions.
        validateTree(source);
        for (Path target : targets) validateTree(target);
        if (!Files.exists(source, NO_LINKS)) {
            for (Path target : targets) {
                if (Files.exists(target, NO_LINKS)) {
                    throw new IOException("Missing Realm 1 template " + source + "; existing Realm saves were preserved");
                }
            }
            // First boot: all three empty void saves will receive the same initial platform.
            Files.createDirectory(source);
        }
        Files.createDirectory(transaction);
        boolean[] backedUp = new boolean[targets.length];
        boolean[] installed = new boolean[targets.length];
        try {
            for (int i = 0; i < targets.length; i++) {
                Path staged = transaction.resolve("new-" + targets[i].getFileName());
                copyTree(source, staged);
                // Forge names village storage using DimensionType's suffix, even inside DIM folders.
                Path village = staged.resolve("data/villages_revolution_realm_1.dat");
                if (Files.exists(village, NO_LINKS)) {
                    Files.move(village, village.resolveSibling("villages_revolution_realm_" + (i + 2) + ".dat"));
                }
            }
            for (int i = 0; i < targets.length; i++) {
                if (Files.exists(targets[i], NO_LINKS)) {
                    Files.move(targets[i], transaction.resolve("old-" + targets[i].getFileName()));
                    backedUp[i] = true;
                }
                Files.move(transaction.resolve("new-" + targets[i].getFileName()), targets[i]);
                installed[i] = true;
            }
        } catch (IOException | RuntimeException e) {
            // A failed publish restores both old destinations. Keep recovery files if rollback fails.
            try {
                for (int i = targets.length - 1; i >= 0; i--) {
                    if (installed[i]) deleteTree(targets[i]);
                    if (backedUp[i]) Files.move(transaction.resolve("old-" + targets[i].getFileName()), targets[i]);
                }
                deleteTree(transaction);
            } catch (IOException rollbackFailure) { e.addSuppressed(rollbackFailure); }
            throw e;
        }
        // A crash or cleanup error leaves the transaction directory and blocks the next startup.
        deleteTree(transaction);
    }

    private static Path dimension(Path root, int realmId) {
        int id = RealmCatalog.byRealmId(realmId).getDimensionId();
        if (id == 0) throw new IllegalStateException("Overworld must never participate in Realm sync");
        return root.resolve("DIM" + id);
    }

    private static void rejectLink(Path path) throws IOException {
        if (Files.exists(path, NO_LINKS)) {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, NO_LINKS);
            if (attributes.isSymbolicLink() || attributes.isOther()) throw new IOException("Unsupported link/special file: " + path);
        }
    }

    private static void validateTree(Path tree) throws IOException {
        if (!Files.exists(tree, NO_LINKS)) return;
        rejectLink(tree);
        if (!Files.isDirectory(tree, NO_LINKS)) throw new IOException("Expected dimension directory: " + tree);
        Files.walkFileTree(tree, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                rejectLink(dir);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile()) throw new IOException("Unsupported dimension file: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyTree(final Path source, final Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                rejectLink(dir);
                Files.createDirectory(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile()) throw new IOException("Unsupported dimension file: " + file);
                Files.copy(file, target.resolve(source.relativize(file)), NO_LINKS);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteTree(Path tree) throws IOException {
        // All callers pass only a validated DIM202/203 or this save's transaction directory.
        validateTree(tree);
        Files.walkFileTree(tree, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                if (failure != null) throw failure;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override public void close() throws IOException {
        try { lock.release(); }
        finally { channel.close(); }
    }
}
