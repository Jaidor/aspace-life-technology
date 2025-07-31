package com.aspacelifetechnology.aspace_life_technology.controller.dtos;

public class PostDto {
    private Long userId;
    private Long id;
    private String title;
    private String body;
    // getters + setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
