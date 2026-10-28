package pl.zrcwaw.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zrcwaw.chat.model.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, String> {
}
