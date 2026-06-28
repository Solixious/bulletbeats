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

    public String upload(byte[] bytes, String contentType, String objectPath) {
        supabaseRestClient.post()
                .uri(objectUri(objectPath))
                .header("Authorization", "Bearer " + props.getServiceRoleKey())
                .header("x-upsert", "true")
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
