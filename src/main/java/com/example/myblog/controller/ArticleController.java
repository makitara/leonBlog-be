package com.example.myblog.controller;

import com.example.myblog.data.BlogDataStore;
import com.example.myblog.model.ArticleDetail;
import com.example.myblog.model.ArticleSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final BlogDataStore dataStore;

    public ArticleController(BlogDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping
    public List<ArticleSummary> listArticles() {
        return dataStore.getArticleSummaries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetail> getArticle(@PathVariable String id) {
        return dataStore.getArticleById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
