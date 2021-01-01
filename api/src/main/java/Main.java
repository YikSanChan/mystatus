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
    CqlSession cqlSession = CqlSession.builder().build();
    runCassandraMigration(cqlSession);
    Javalin app =
        Javalin.create(config -> config.registerPlugin(new OpenApiPlugin(getOpenApiOptions())))
            .start(PORT);
    Handlers handlers = new Handlers(cqlSession);
    registerAppRoutes(app, handlers);
  }

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

  private static void registerAppRoutes(Javalin app, Handlers handlers) {
    app.get("users", handlers::getAllUsers);
    app.get("users/:username", handlers::getUser);
    app.get(":username/home", handlers::home);
    app.get(":username/status", handlers::getAllStatuses);
    app.post(":username/status", handlers::createStatus);
    app.get(":username/status/:status_id", handlers::getStatus);
    app.delete(":username/status/:status_id", handlers::deleteStatus);
    app.post(":username/follow/:followee_username", handlers::follow);
    app.delete(":username/follow/:followee_username", handlers::unfollow);
    app.get(":username/following", handlers::following);
    app.get(":username/followed_by", handlers::followedBy);
  }
}
