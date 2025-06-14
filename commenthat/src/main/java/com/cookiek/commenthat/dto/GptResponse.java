package com.cookiek.commenthat.dto;

import lombok.Data;

import java.util.List;

@Data
public class GptResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
