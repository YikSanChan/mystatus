import com.datastax.oss.driver.api.core.CqlSession;
import io.javalin.Javalin;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Info;
import org.cognitor.cassandra.migration.Database;
import org.cognitor.cassandra.migration.MigrationRepository;
import org.cognitor.cassandra.migration.MigrationTask;
import org.cognitor.cassandra.migration.keyspace.Keyspace;

public class Main {

  private static final int PORT = 7000;
  private static final String NAME = "my_status";

  public static void main(String[] args) {
    try (CqlSession cqlSession = CqlSession.builder().build()) {
      runCassandraMigration(cqlSession);
    }
    Javalin app =
        Javalin.create(config -> config.registerPlugin(new OpenApiPlugin(getOpenApiOptions())))
            .start(PORT);

    app.get("users", Handlers::getAllUsers);
    app.get("users/:username", Handlers::getUser);
    app.get(":username/home", Handlers::home);
    app.get(":username/status", Handlers::getAllStatuses);
    app.post(":username/status", Handlers::createStatus);
    app.get(":username/status/:status_id", Handlers::getStatus);
    app.delete(":username/status/:status_id", Handlers::deleteStatus);
    app.post(":username/follow/:followee_username", Handlers::follow);
    app.delete(":username/follow/:followee_username", Handlers::unfollow);
    app.get(":username/following", Handlers::following);
    app.get(":username/followed_by", Handlers::followedBy);
  }

  // TODO: should I reuse the session? probably not. should I use a threadpool? maybe yes.
  private static void runCassandraMigration(CqlSession cqlSession) {
    Database database = new Database(cqlSession, new Keyspace(NAME));
    MigrationTask migration = new MigrationTask(database, new MigrationRepository());
    migration.migrate();
  }

  private static OpenApiOptions getOpenApiOptions() {
    Info applicationInfo = new Info().version("1.0").description("My Application");
    return new OpenApiOptions(applicationInfo)
        .path("/swagger-docs")
        .swagger(new SwaggerOptions("/swagger").title("My Swagger Documentation"));
  }
}
