package com.pddh.temples_service.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.temples_service.api_common.resilience.RateLimitGuard;

@RestController
public class ImageUrlResolverController {
    private static final Pattern OG_IMAGE = Pattern.compile(
            "<meta[^>]+property=[\\\"']og:image[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']|"
                    + "<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+property=[\\\"']og:image[\\\"']",
            Pattern.CASE_INSENSITIVE);
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final RateLimitGuard rateLimitGuard;

    public ImageUrlResolverController(RateLimitGuard rateLimitGuard) {
        this.rateLimitGuard = rateLimitGuard;
    }

    @GetMapping("${app.content.path}/resolve-image")
    public CompletableFuture<ResolvedImage> resolve(@RequestParam("url") String value) {
        rateLimitGuard.check("image.url.resolve");
        URI page = parseUnsplashPhotoPage(value);
        HttpRequest request = HttpRequest.newBuilder(page)
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "PawanDhamImageResolver/1.0")
                .header("Accept", "text/html")
                .GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApply(response -> {
            if (response.statusCode() != 200) {
                try { response.body().close(); } catch (IOException ignored) { }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The public photo page could not be opened");
            }
            String html;
            try (var body = response.body()) {
                html = new String(body.readNBytes(1_000_000), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
            Matcher matcher = OG_IMAGE.matcher(html);
            if (!matcher.find()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "The photo page does not provide a preview image");
            }
            String imageUrl = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            URI image = URI.create(imageUrl.replace("&amp;", "&"));
            if (!"https".equalsIgnoreCase(image.getScheme()) || !"images.unsplash.com".equalsIgnoreCase(image.getHost())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The photo page returned an unsupported image host");
            }
            return new ResolvedImage(image.toString());
        }).exceptionallyCompose(error -> {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (cause instanceof ResponseStatusException statusException) return CompletableFuture.failedFuture(statusException);
            return CompletableFuture.failedFuture(new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Could not load the public photo page", cause));
        });
    }

    private URI parseUnsplashPhotoPage(String value) {
        try {
            if (value == null || value.length() > 2048) throw new IllegalArgumentException();
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || !("unsplash.com".equalsIgnoreCase(host) || "www.unsplash.com".equalsIgnoreCase(host))
                    || uri.getPort() != -1 || uri.getPath() == null || !uri.getPath().startsWith("/photos/")) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only public Unsplash photo page URLs can be resolved. For other sites, use a direct image URL or upload the image.", exception);
        }
    }

    public record ResolvedImage(String imageUrl) { }
}
