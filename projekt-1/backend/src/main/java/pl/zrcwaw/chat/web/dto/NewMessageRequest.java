package pl.zrcwaw.chat.web.dto;

public record NewMessageRequest(String username, String content, String fileId) {
}
