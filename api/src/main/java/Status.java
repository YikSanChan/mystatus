import java.util.UUID;

public class Status {
    private final String username;
    private final UUID id;
    private final String body;

    public Status(String username, UUID id, String body) {
        this.username = username;
        this.id = id;
        this.body = body;
    }

    public String getUsername() {
        return username;
    }

    public UUID getId() {
        return id;
    }

    public String getBody() {
        return body;
    }
}
