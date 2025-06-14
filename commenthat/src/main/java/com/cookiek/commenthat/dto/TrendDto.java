package com.cookiek.commenthat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrendDto {
    private String word;
    private Integer count;
}
