package com.example.myblog.model;

public record ArticleDetail(
        String id,
        String title,
        String publishDate,
        String content
) {
}
