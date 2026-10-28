package pl.zrcwaw.chat.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstrakcja nad miejscem przechowywania plikow.
 * Lokalnie: katalog na dysku (LocalFileStorage). W chmurze: AWS S3 (implementacja dodawana w kroku 7 projektu).
 */
public interface FileStorage {

    void store(String key, InputStream content, long size, String contentType) throws IOException;

    Resource load(String key) throws IOException;
}
