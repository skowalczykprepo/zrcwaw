package pl.zrcwaw.chat.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.local.dir}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) throws IOException {
        Files.copy(content, root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Resource load(String key) throws IOException {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new IOException("File not found: " + key);
        }
        return new PathResource(path);
    }
}
