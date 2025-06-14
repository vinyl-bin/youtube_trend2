package com.cookiek.commenthat.repository;

import com.cookiek.commenthat.domain.YoutubeRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface YoutubeRawRepository extends JpaRepository<YoutubeRaw, Long> {
    List<YoutubeRaw> findAllByTimeBetween(LocalDateTime start, LocalDateTime end);
}
