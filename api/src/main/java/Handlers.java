import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import io.javalin.http.Context;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO: all() return paginated results

public class Handlers {
  public static Context getUser(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      SimpleStatement s =
          SimpleStatement.newInstance("select * from my_status.users where username = ?", username);
      ResultSet rs = session.execute(s);
      Row row = rs.one();
      if (row == null) {
        return ctx.status(403);
      }
      return ctx.json(rowToUser(row));
    }
  }

  public static Context getAllUsers(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      ResultSet rs = session.execute("select * from my_status.users");
      List<Row> rows = rs.all();
      return ctx.json(rows.stream().map(Handlers::rowToUser).collect(Collectors.toList()));
    }
  }

  public static Context home(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      SimpleStatement s =
          SimpleStatement.newInstance(
              "select * from my_status.home_status_updates where timeline_username = ?", username);
      ResultSet rs = session.execute(s);
      List<Row> rows = rs.all();
      return ctx.json(
          rows.stream()
              .map(
                  r ->
                      new Status(
                          r.getString("status_update_username"),
                          r.getUuid("status_update_id"),
                          r.getString("body")))
              .collect(Collectors.toList()));
    }
  }

  public static Context follow(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String follower = ctx.pathParam("username");
      String followee = ctx.pathParam("followee_username");

      SimpleStatement getFolloweeStatuses =
          SimpleStatement.newInstance(
              "select * from my_status.user_status_updates where username = ?", followee);
      List<Row> followeeStatuses = session.execute(getFolloweeStatuses).all();
      List<BatchableStatement<?>> insertHomeStatusUpdates =
          followeeStatuses.stream()
              .map(
                  r ->
                      SimpleStatement.newInstance(
                          "insert into my_status.home_status_updates (timeline_username, status_update_id, status_update_username, body) values (?, ?, ?, ?)",
                          follower,
                          r.getUuid("id"),
                          r.getString("username"),
                          r.getString("body")))
              .collect(Collectors.toList());
      SimpleStatement insertUserInboundFollow =
          SimpleStatement.newInstance(
              "insert into my_status.user_inbound_follows (followed_username, follower_username) values (?, ?)",
              followee,
              follower);
      SimpleStatement insertUserOutboundFollow =
          SimpleStatement.newInstance(
              "insert into my_status.user_outbound_follows (follower_username, followed_username) values (?, ?)",
              follower,
              followee);
      BatchStatement batch =
          BatchStatement.builder(DefaultBatchType.LOGGED)
              .addStatement(insertUserInboundFollow)
              .addStatement(insertUserOutboundFollow)
              .addStatements(insertHomeStatusUpdates)
              .build();
      session.execute(batch);
      return ctx.result("Followed");
    }
  }

  public static Context unfollow(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String follower = ctx.pathParam("username");
      String followee = ctx.pathParam("followee_username");

      SimpleStatement getFolloweeStatusIds =
          SimpleStatement.newInstance(
              "select id from my_status.user_status_updates where username = ?", followee);
      List<UUID> followeeStatusIds =
          session.execute(getFolloweeStatusIds).all().stream()
              .map(r -> r.getUuid("id"))
              .collect(Collectors.toList());

      SimpleStatement deleteUserInboundFollow =
          SimpleStatement.newInstance(
              "delete from my_status.user_inbound_follows where followed_username = ? and follower_username = ?",
              followee,
              follower);
      SimpleStatement deleteUserOutboundFollow =
          SimpleStatement.newInstance(
              "delete from my_status.user_outbound_follows where follower_username = ? and followed_username = ?",
              follower,
              followee);
      SimpleStatement deleteHomeStatusUpdates =
          SimpleStatement.newInstance(
              "delete from my_status.home_status_updates where timeline_username = ? and status_update_id in ?",
              follower,
              followeeStatusIds);
      BatchStatement batch =
          BatchStatement.builder(DefaultBatchType.LOGGED)
              .addStatement(deleteUserInboundFollow)
              .addStatement(deleteUserOutboundFollow)
              .addStatement(deleteHomeStatusUpdates)
              .build();
      session.execute(batch);
      return ctx.result("Unfollowed");
    }
  }

  public static Context following(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      SimpleStatement s =
          SimpleStatement.newInstance(
              "select followed_username from my_status.user_outbound_follows where follower_username = ?",
              username);
      List<Row> rows = session.execute(s).all();
      return ctx.json(
          rows.stream().map(r -> r.getString("followed_username")).collect(Collectors.toList()));
    }
  }

  public static Context followedBy(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      SimpleStatement s =
          SimpleStatement.newInstance(
              "select follower_username from my_status.user_inbound_follows where followed_username = ?",
              username);
      List<Row> rows = session.execute(s).all();
      return ctx.json(
          rows.stream().map(r -> r.getString("follower_username")).collect(Collectors.toList()));
    }
  }

  public static Context getAllStatuses(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      SimpleStatement s =
          SimpleStatement.newInstance(
              "select * from my_status.user_status_updates where username = ?", username);
      ResultSet rs = session.execute(s);
      List<Row> rows = rs.all();
      return ctx.json(rows.stream().map(Handlers::rowToStatus).collect(Collectors.toList()));
    }
  }

  public static Context getStatus(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      String statusId = ctx.pathParam("status_id");
      SimpleStatement s =
          SimpleStatement.newInstance(
              "select * from my_status.user_status_updates where username = ? and id = ?",
              username,
              statusId);
      ResultSet rs = session.execute(s);
      Row row = rs.one();
      if (row == null) {
        return ctx.status(403);
      }
      return ctx.json(rowToStatus(row));
    }
  }

  public static Context createStatus(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      String body = ctx.body();

      // any non-empty table will work
      SimpleStatement getNow =
          SimpleStatement.newInstance("select NOW() from my_status.users limit 1");
      UUID now = session.execute(getNow).one().getUuid(0);

      SimpleStatement getFollowers =
          SimpleStatement.newInstance(
              "select follower_username from my_status.user_inbound_follows where followed_username = ?",
              username);
      List<String> followers =
          session.execute(getFollowers).all().stream()
              .map(r -> r.getString("follower_username"))
              .collect(Collectors.toList());

      PreparedStatement insertHomeStatusPrepare =
          session.prepare(
              "insert into my_status.home_status_updates (timeline_username, status_update_id, status_update_username, body) values (:timeline_username, :status_updated_id, :status_update_username, :body)");
      Iterable<BatchableStatement<?>> multiInsertHomeStatus =
          followers.stream()
              .map(f -> insertHomeStatusPrepare.bind(f, now, username, body))
              .collect(Collectors.toList());

      SimpleStatement insertUserStatus =
          SimpleStatement.newInstance(
              "insert into my_status.user_status_updates (username, id, body) values (?, ?, ?)",
              username,
              now,
              body);

      BatchStatement batch =
          BatchStatement.builder(DefaultBatchType.LOGGED)
              .addStatement(insertUserStatus)
              .addStatements(multiInsertHomeStatus)
              .build();

      ResultSet rs = session.execute(batch);
      return ctx.result("Created status");
    }
  }

  public static Context deleteStatus(Context ctx) {
    try (CqlSession session = CqlSession.builder().build()) {
      String username = ctx.pathParam("username");
      String statusId = ctx.pathParam("status_id");
      SimpleStatement getFollowers =
          SimpleStatement.newInstance(
              "select follower_username from my_status.user_inbound_follows where followed_username = ?",
              username);
      List<String> followers =
          session.execute(getFollowers).all().stream()
              .map(r -> r.getString("follower_username"))
              .collect(Collectors.toList());
      SimpleStatement deleteHomeStatusUpdates =
          SimpleStatement.newInstance(
              "delete from my_status where timeline_username in ? and status_update_id = ?",
              followers,
              statusId);
      SimpleStatement deleteUserStatusUpdate =
          SimpleStatement.newInstance(
              "delete from my_status.user_status_updates where username = ? and id = ?",
              username,
              statusId);
      BatchStatement batch =
          BatchStatement.builder(DefaultBatchType.LOGGED)
              .addStatement(deleteUserStatusUpdate)
              .addStatement(deleteHomeStatusUpdates)
              .build();
      session.execute(batch);
      return ctx.result("Deleted status");
    }
  }

  private static User rowToUser(Row row) {
    return new User(row.getString("username"), row.getString("email"));
  }

  private static Status rowToStatus(Row row) {
    return new Status(row.getString("username"), row.getUuid("id"), row.getString("body"));
  }
}
