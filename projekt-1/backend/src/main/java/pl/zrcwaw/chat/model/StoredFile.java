package pl.zrcwaw.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Metadane przeslanego pliku; sama zawartosc lezy w FileStorage (dysk lokalny / S3). */
@Entity
@Table(name = "files")
public class StoredFile {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private Instant uploadedAt;

    protected StoredFile() {
    }

    public StoredFile(String id, String originalName, String contentType, long size) {
        this.id = id;
        this.originalName = originalName;
        this.contentType = contentType;
        this.size = size;
        this.uploadedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
