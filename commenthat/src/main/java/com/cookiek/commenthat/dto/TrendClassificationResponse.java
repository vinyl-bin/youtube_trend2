package com.cookiek.commenthat.dto;

import lombok.Data;

@Data
public class TrendClassificationResponse {
    private String word;
    private Boolean stopwordCandidate;
    private String note;
    private String category;
}
