package com.example.myblog.controller;

import com.example.myblog.data.BlogDataStore;
import com.example.myblog.model.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final BlogDataStore dataStore;

    public ProfileController(BlogDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping
    public Profile getProfile() {
        return dataStore.getProfile();
    }
}
