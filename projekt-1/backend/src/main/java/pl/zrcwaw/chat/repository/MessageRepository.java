package pl.zrcwaw.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zrcwaw.chat.model.Message;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByOrderByCreatedAtAsc();

    List<Message> findByCreatedAtAfterOrderByCreatedAtAsc(Instant after);
}
