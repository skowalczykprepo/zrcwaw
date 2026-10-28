package pl.zrcwaw.chat.web.dto;

import pl.zrcwaw.chat.model.StoredFile;

public record FileDto(String id, String name, String contentType, long size, String url) {

    public static FileDto from(StoredFile f) {
        return new FileDto(f.getId(), f.getOriginalName(), f.getContentType(), f.getSize(), "/api/files/" + f.getId());
    }
}
