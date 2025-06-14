package com.cookiek.commenthat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter @Setter
public class CategoryTrendDto{

    public CategoryTrendDto() {}  // 기본 생성자 (필수)

    public CategoryTrendDto(String category, List<TrendItemDto> trends) {
        this.category = category;
        this.trends = trends;
    }

    String category;
    List<TrendItemDto> trends;
}
