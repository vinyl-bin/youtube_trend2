package com.cookiek.commenthat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_raw")
@RequiredArgsConstructor
@Getter
@Setter
public class YoutubeRaw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long youtubeRawId;

    @Column(nullable = false)
    private LocalDateTime time;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "des", nullable = false, length = 500)
    private String des;
}
