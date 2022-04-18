package cli.net;

import io.mikael.urlbuilder.UrlBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class Request {

    private final URI baseUri;
    private HttpClient client = HttpClient.newBuilder().build();

    public Request(URI baseUri) {
        this.baseUri = baseUri;
    }

    public HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder()
                .uri(baseUri.resolve(path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> get(String path, @Nullable Map<String, String> query) throws IOException, InterruptedException {
        var builder = UrlBuilder.fromUri(baseUri.resolve(path));
        if (query != null) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                builder = builder.addParameter(entry.getKey(), entry.getValue());
            }
        }

        var req = HttpRequest.newBuilder()
                .uri(builder.toUri())
                .GET()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
