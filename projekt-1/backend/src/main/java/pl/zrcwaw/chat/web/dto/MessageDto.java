package pl.zrcwaw.chat.web.dto;

import pl.zrcwaw.chat.model.Message;

import java.time.Instant;

public record MessageDto(Long id, String username, String content, String fileId, int likes, Instant createdAt) {

    public static MessageDto from(Message m) {
        return new MessageDto(m.getId(), m.getUsername(), m.getContent(), m.getFileId(), m.getLikes(), m.getCreatedAt());
    }
}
