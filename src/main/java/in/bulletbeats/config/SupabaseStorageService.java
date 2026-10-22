package in.bulletbeats.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseProperties props;
    private final RestClient supabaseRestClient;

    // Supabase stores this value verbatim as the object's Cache-Control (prefixed with "public, ").
    // Without the header it defaults to "no-cache", forcing a revalidation request on every view.
    public static final String CACHE_IMMUTABLE = "max-age=31536000, immutable";
    private static final String CACHE_NONE = "no-cache";

    public String upload(byte[] bytes, String contentType, String objectPath) {
        return upload(bytes, contentType, objectPath, CACHE_NONE);
    }

    /** Use CACHE_IMMUTABLE only for objects whose path is never reused for different content. */
    public String upload(byte[] bytes, String contentType, String objectPath, String cacheControl) {
        supabaseRestClient.post()
                .uri(objectUri(objectPath))
                .header("Authorization", "Bearer " + props.getServiceRoleKey())
                .header("x-upsert", "true")
                .header("cache-control", cacheControl)
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes)
                .retrieve()
                .toBodilessEntity();
        return publicUrl(objectPath);
    }

    public void delete(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) return;
        supabaseRestClient.method(HttpMethod.DELETE)
                .uri(props.getUrl() + "/storage/v1/object/" + props.getBucket())
                .header("Authorization", "Bearer " + props.getServiceRoleKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("prefixes", List.of(objectPath)))
                .retrieve()
                .toBodilessEntity();
    }

    public byte[] download(String url) {
        return supabaseRestClient.get()
                .uri(url)
                .retrieve()
                .body(byte[].class);
    }

    public String publicUrl(String objectPath) {
        return props.getUrl() + "/storage/v1/object/public/" + props.getBucket() + "/" + objectPath;
    }

    public String extractObjectPath(String publicUrl) {
        String prefix = props.getUrl() + "/storage/v1/object/public/" + props.getBucket() + "/";
        if (publicUrl != null && publicUrl.startsWith(prefix)) {
            return publicUrl.substring(prefix.length());
        }
        return publicUrl;
    }

    private String objectUri(String objectPath) {
        return props.getUrl() + "/storage/v1/object/" + props.getBucket() + "/" + objectPath;
    }
}
