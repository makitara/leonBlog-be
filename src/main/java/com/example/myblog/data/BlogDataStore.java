package com.example.myblog.data;

import com.example.myblog.config.ProfileConfig;
import com.example.myblog.model.ArticleDetail;
import com.example.myblog.model.ArticleSummary;
import com.example.myblog.model.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class BlogDataStore {

    public static final String ASSET_URL_PREFIX = "/blog-assets/";
    private static final String PROFILE_FILE_NAME = "profile.json";
    private static final String ARTICLES_DIR_NAME = "articles";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Path dataRoot;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public BlogDataStore(@Value("${blog.data.path:}") String dataPath,
                         @Value("${blog.base-url:}") String baseUrl,
                         ObjectMapper objectMapper) {
        if (dataPath == null || dataPath.isBlank()) {
            throw new IllegalStateException(
                    "缺少 blog.data.path 配置，请参考 README 设置数据目录");
        }
        this.dataRoot = Path.of(dataPath).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public Profile getProfile() {
        ProfileConfig profileConfig = readProfileConfig();
        return new Profile(
                profileConfig.id(),
                profileConfig.username(),
                buildAvatarUrl(profileConfig.avatar()),
                profileConfig.bio(),
                profileConfig.email()
        );
    }

    public List<ArticleSummary> getArticleSummaries() {
        return loadArticles().stream()
                .map(article -> new ArticleSummary(
                        article.id(),
                        article.title(),
                        article.publishDate()
                ))
                .toList();
    }

    public Optional<ArticleDetail> getArticleById(String id) {
        return loadArticles().stream()
                .filter(article -> article.id().equals(id))
                .findFirst()
                .map(article -> new ArticleDetail(
                        article.id(),
                        article.title(),
                        article.publishDate(),
                        article.content()
                ));
    }

    public Path assetsDirectory() {
        return dataRoot.resolve("assets");
    }

    public Path dataRoot() {
        return dataRoot;
    }

    private ProfileConfig readProfileConfig() {
        Path profilePath = dataRoot.resolve(PROFILE_FILE_NAME);
        try {
            return objectMapper.readValue(profilePath.toFile(), ProfileConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取个人资料配置: " + profilePath, e);
        }
    }

    private List<ArticleFile> loadArticles() {
        Path articlesDir = dataRoot.resolve(ARTICLES_DIR_NAME);
        if (!Files.isDirectory(articlesDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(articlesDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".md"))
                    .map(this::readArticleFile)
                    .sorted((a, b) -> b.publishDate().compareTo(a.publishDate()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("无法读取文章目录: " + articlesDir, e);
        }
    }

    private ArticleFile readArticleFile(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            FrontMatter frontMatter = extractFrontMatter(lines);
            List<String> contentLines = lines.subList(frontMatter.nextLineIndex(), lines.size());
            String content = joinLines(contentLines);

            String fileNameStem = stripExtension(filePath.getFileName().toString());
            String id = chooseValue(frontMatter.meta().get("id"), fileNameStem);
            String title = chooseValue(frontMatter.meta().get("title"),
                    findFirstHeading(contentLines).orElse(fileNameStem));
            String publishDate = chooseValue(frontMatter.meta().get("publishDate"),
                    formatModifiedDate(Files.getLastModifiedTime(filePath)));

            return new ArticleFile(id, title, publishDate, content);
        } catch (IOException e) {
            throw new IllegalStateException("无法解析文章文件: " + filePath, e);
        }
    }

    private FrontMatter extractFrontMatter(List<String> lines) {
        if (lines.isEmpty() || !lines.get(0).trim().equals("---")) {
            return new FrontMatter(Map.of(), 0);
        }
        Map<String, String> meta = new LinkedHashMap<>();
        int index = 1;
        while (index < lines.size()) {
            String line = lines.get(index).trim();
            if (line.equals("---")) {
                index++;
                break;
            }
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                meta.put(key, value);
            }
            index++;
        }
        return new FrontMatter(meta, Math.min(index, lines.size()));
    }

    private Optional<String> findFirstHeading(List<String> contentLines) {
        return contentLines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("#"))
                .findFirst()
                .map(line -> line.replaceFirst("^#+\\s*", "").trim())
                .filter(line -> !line.isBlank());
    }

    private String formatModifiedDate(FileTime fileTime) {
        LocalDate date = fileTime.toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        return DATE_FORMATTER.format(date);
    }

    private String joinLines(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            builder.append(lines.get(i));
            if (i < lines.size() - 1) {
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString().stripLeading();
    }

    private String stripExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(0, index) : filename;
    }

    private String chooseValue(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        return candidate;
    }

    private String buildAvatarUrl(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) {
            return "";
        }
        String relative = ASSET_URL_PREFIX + normalizeRelativePath(avatarPath);
        if (baseUrl.isBlank()) {
            return relative;
        }
        String sanitizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return sanitizedBase + relative;
    }

    private String normalizeRelativePath(String path) {
        return path.replace("\\", "/").replaceAll("^/+", "");
    }

    private record ArticleFile(
            String id,
            String title,
            String publishDate,
            String content
    ) {
    }

    private record FrontMatter(
            Map<String, String> meta,
            int nextLineIndex
    ) {
    }
}
