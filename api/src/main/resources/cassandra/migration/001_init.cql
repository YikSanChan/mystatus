CREATE TABLE "users"
(
    "username"           text,
    "email"              text,
    "encrypted_password" blob,
    PRIMARY KEY ("username")
);

INSERT INTO "users"
    ("username", "email", "encrypted_password")
VALUES ('alice',
        'alice@gmail.com', 0x8914977ed729792e403da53024c6069a9158b8c4);
INSERT INTO "users"
    ("username", "encrypted_password")
VALUES ('bob',
        0x10920941a69549d33aaee6116ed1f47e19b8e713);
INSERT INTO "users"
    ("username", "email", "encrypted_password")
VALUES ('carol',
        'carol@gmail.com', 0xed3d8299b191b59b7008759a104c10af3db6e63a);
INSERT INTO "users"
    ("username", "email", "encrypted_password")
VALUES ('dave',
        'dave@gmail.com', 0x6d1d90d92bbab0012270536f286d243729690a5b);

CREATE TABLE "user_status_updates"
(
    "username" text,
    "id"       timeuuid,
    "body"     text,
    PRIMARY KEY ("username", "id")
);

CREATE TABLE "user_outbound_follows"
(
    "follower_username" text,
    "followed_username" text,
    PRIMARY KEY ("follower_username", "followed_username")
);

CREATE TABLE "user_inbound_follows"
(
    "followed_username" text,
    "follower_username" text,
    PRIMARY KEY ("followed_username", "follower_username")
);

CREATE TABLE "home_status_updates"
(
    "timeline_username"      text,
    "status_update_id"       timeuuid,
    "status_update_username" text,
    "body"                   text,
    PRIMARY KEY ("timeline_username", "status_update_id")
) WITH CLUSTERING ORDER BY ("status_update_id" DESC);
