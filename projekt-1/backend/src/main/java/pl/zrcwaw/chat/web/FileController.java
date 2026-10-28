package pl.zrcwaw.chat.web;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pl.zrcwaw.chat.model.StoredFile;
import pl.zrcwaw.chat.repository.StoredFileRepository;
import pl.zrcwaw.chat.storage.FileStorage;
import pl.zrcwaw.chat.web.dto.FileDto;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StoredFileRepository files;
    private final FileStorage storage;

    public FileController(StoredFileRepository files, FileStorage storage) {
        this.files = files;
        this.storage = storage;
    }

    /** POST /api/files (multipart/form-data, pole "file") - przeslanie pliku multimedialnego. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is empty");
        }
        String id = UUID.randomUUID().toString();
        String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : id;

        storage.store(id, file.getInputStream(), file.getSize(), contentType);
        StoredFile saved = files.save(new StoredFile(id, name, contentType, file.getSize()));

        return ResponseEntity.created(URI.create("/api/files/" + id)).body(FileDto.from(saved));
    }

    /** GET /api/files/{id} - pobranie pliku. */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) throws IOException {
        StoredFile meta = files.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found"));
        Resource resource = storage.load(meta.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getOriginalName() + "\"")
                .body(resource);
    }
}
