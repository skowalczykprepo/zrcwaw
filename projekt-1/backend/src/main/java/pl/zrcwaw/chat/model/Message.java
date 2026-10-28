package pl.zrcwaw.chat.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 2000)
    private String content;

    /** Opcjonalny zalacznik - identyfikator pliku z tabeli files. */
    @Column(length = 36)
    private String fileId;

    @Column(nullable = false)
    private int likes;

    @Column(nullable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(String username, String content, String fileId) {
        this.username = username;
        this.content = content;
        this.fileId = fileId;
        this.likes = 0;
        this.createdAt = Instant.now();
    }

    public void like() {
        this.likes++;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public String getFileId() {
        return fileId;
    }

    public int getLikes() {
        return likes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
