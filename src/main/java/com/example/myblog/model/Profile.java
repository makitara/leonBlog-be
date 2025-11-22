package com.example.myblog.model;

public record Profile(
        String id,
        String username,
        String avatarUrl,
        String bio,
        String email
) {
}
