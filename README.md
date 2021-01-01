# MyStatus

## Dev setup

Spin up the cassandra db.

```
cd api
docker-compose up -d
```

(optional) cqlsh into the cassandra instance.

```
docker exec -it mystatus_cassandra cqlsh
```

Run the app in IntelliJ.
