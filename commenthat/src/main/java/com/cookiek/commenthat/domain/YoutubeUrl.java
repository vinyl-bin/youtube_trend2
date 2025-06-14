package com.cookiek.commenthat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "youtube_url")
@Getter
@Setter
@NoArgsConstructor
public class YoutubeUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long youtubeUrlId;



}
