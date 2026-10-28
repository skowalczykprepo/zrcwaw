package pl.zrcwaw.chat.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pl.zrcwaw.chat.model.Message;
import pl.zrcwaw.chat.repository.MessageRepository;
import pl.zrcwaw.chat.repository.StoredFileRepository;
import pl.zrcwaw.chat.web.dto.MessageDto;
import pl.zrcwaw.chat.web.dto.NewMessageRequest;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    /** Sztuczne opoznienie uzywane do symulacji spadku wydajnosci (punkt 10 - CloudWatch). */
    private static final long SLOW_DELAY_MS = 3000;

    private final MessageRepository messages;
    private final StoredFileRepository files;

    public MessageController(MessageRepository messages, StoredFileRepository files) {
        this.messages = messages;
        this.files = files;
    }

    /** GET /api/messages[?slow=true] - wszystkie wiadomosci. */
    @GetMapping
    public List<MessageDto> getAll(@RequestParam(defaultValue = "false") boolean slow) {
        if (slow) {
            simulateSlowResponse();
        }
        return messages.findAllByOrderByCreatedAtAsc().stream().map(MessageDto::from).toList();
    }

    /** GET /api/messages/new?after=2026-10-25T12:00:00Z - wiadomosci nowsze niz podany znacznik czasu. */
    @GetMapping("/new")
    public List<MessageDto> getNew(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant after) {
        return messages.findByCreatedAtAfterOrderByCreatedAtAsc(after).stream().map(MessageDto::from).toList();
    }

    /** POST /api/messages - wyslanie nowej wiadomosci (opcjonalnie z zalacznikiem fileId). */
    @PostMapping
    public ResponseEntity<MessageDto> create(@RequestBody NewMessageRequest request) {
        if (isBlank(request.username()) || isBlank(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and content are required");
        }
        if (request.fileId() != null && !files.existsById(request.fileId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown fileId");
        }
        Message saved = messages.save(new Message(request.username().trim(), request.content().trim(), request.fileId()));
        return ResponseEntity.created(URI.create("/api/messages/" + saved.getId())).body(MessageDto.from(saved));
    }

    /** POST /api/messages/{id}/like - polubienie wiadomosci. */
    @PostMapping("/{id}/like")
    public MessageDto like(@PathVariable Long id) {
        Message message = messages.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "message not found"));
        message.like();
        return MessageDto.from(messages.save(message));
    }

    private static void simulateSlowResponse() {
        try {
            Thread.sleep(SLOW_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
