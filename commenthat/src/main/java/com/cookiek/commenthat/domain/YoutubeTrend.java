package com.cookiek.commenthat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_trend")
@Getter
@Setter
@NoArgsConstructor
public class YoutubeTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trendId;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false)
    private Integer count;

    @Column(nullable = false)
    private LocalDateTime periodStart;

    @Column(nullable = false)
    private LocalDateTime periodEnd;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean stopwordCandidate = false;

    @Column(length = 255)
    private String note;

    private String category;

}
