package com.cookiek.commenthat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class GptRequest {
    private String model;
    private List<Message> messages;
    private Integer max_tokens;

    @Data
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
