# java-eloquent

`java-eloquent` is a lightweight relation helper for Java 8 applications that use `NamedParameterJdbcTemplate`.
It provides an Eloquent-style API for registering relation metadata, building queries, eager loading related data,
and performing basic write operations without introducing a full ORM layer.

## What It Is For

- registering entity-to-table metadata in code
- defining `hasOne`, `hasMany`, `belongsTo`, `belongsToMany`, and `hasOneThroughPivot` relations
- querying data with a fluent API
- eager loading nested relations
- previewing generated SQL for debugging
- handling simple writes and pivot-table mutations

## Requirements

- Java 8 or newer
- Maven 3.8+
- Spring JDBC

## Project Coordinates

If you want to publish this project as a Maven artifact, the current coordinates are:

```xml
<groupId>io.github.phuonghuu</groupId>
<artifactId>java-eloquent</artifactId>
<version>1.0-SNAPSHOT</version>
```

Before publishing a release, replace the snapshot version with a real release version such as `1.0.0`.

## Installation

If the artifact is already published, add it to your application like this:

```xml
<dependency>
    <groupId>io.github.phuonghuu</groupId>
    <artifactId>java-eloquent</artifactId>
    <version>1.0.0</version>
</dependency>
```

If you are developing locally, build and install it into your local Maven repository first:

```bash
mvn clean install
```

## Building The Project

```bash
mvn clean test
mvn clean package
```

The compiled JAR will be generated under `target/`.

## Main Entry Points

- `RelationKit` is the main facade for reading and writing
- `RelationRegistry` is used to register metadata and relations
- `QueryBuilder` powers fluent queries
- `RelationWriter` handles inserts, updates, and pivot mutations
- `PaginatedResult` and `CursorPaginatedResult` provide paging responses

## Quick Start

### 1. Register metadata

```java
RelationRegistry registry = new RelationRegistry();

registry.forClass(User.class)
    .table("users")
    .primaryKey("id")
    .hasOne("profile", Profile.class, User::getProfileId, Profile::getId, User::setProfile)
    .hasMany("posts", Post.class, User::getId, Post::getUserId, User::setPosts)
    .register();
```

### 2. Create a `RelationKit`

```java
RelationKit kit = RelationKit.of(jdbcTemplate, registry);
```

### 3. Query data

```java
List<User> users = kit.query(User.class)
    .with("profile")
    .where("status", "ACTIVE")
    .orderByDesc("id")
    .get();
```

## Querying

The query API is designed to stay close to SQL while still keeping the code fluent and readable.

Common methods include:

- `select(...)`
- `where(...)` and `orWhere(...)`
- `whereRaw(...)` and `orWhereRaw(...)`
- `whereNull(...)` and `whereNotNull(...)`
- `whereBetween(...)`
- `whereLike(...)`
- `whereDate(...)`
- `whereJson(...)`
- `whereIn(...)`
- `whereHas(...)`
- `with(...)`
- `orderBy(...)` and `orderByDesc(...)`
- `page(...)`, `size(...)`, `limit(...)`, and `offset(...)`
- `first()`
- `find(id)`
- `paginate(...)`
- `cursorPaginate(...)`

### Example

```java
List<User> users = kit.query(User.class)
    .select("id", "username", "status")
    .where("status", "ACTIVE")
    .whereIn("id", java.util.Arrays.asList(1L, 2L, 3L))
    .with("profile")
    .orderBy("id", "DESC")
    .limit(20)
    .get();
```

## Eager Loading

Use `with(...)` to preload relations and reduce the number of database round trips.
Nested paths are supported, for example `with("users.orders")`.

```java
kit.query(Department.class)
    .with("users", q -> q.select("id", "username").orderBy("id").limitPerParent(3))
    .with("users.orders", q -> q.select("id", "amount"))
    .with("manager")
    .get();
```

## Writing Data

Write operations are available through `relationKit.write(Entity.class)`.

Supported operations include:

- `create(entity)`
- `createMany(entities)`
- `update(entity)`
- `updatePartial(entity, ...)`
- `updatePartialByMap(entity, patchValues)`
- `updatePartialById(id, patchValues)`
- `upsert(entity)`
- pivot operations for `belongsToMany`

### Example

```java
RelationWriter<User> writer = kit.write(User.class);

User user = new User();
user.setId(100L);
user.setUsername("alice");
user.setStatus("ACTIVE");

writer.create(user);
```

## Pivot Mutations

Pivot helpers currently focus on `belongsToMany` relations.

Supported methods include:

- `attach(...)`
- `detach(...)`
- `sync(...)`
- `syncWithoutDetaching(...)`
- SQL preview helpers for the same operations

### Example

```java
RelationWriter<User> writer = kit.write(User.class);

writer.attach("roles", 1L, java.util.Arrays.asList(10L, 11L));
writer.sync("roles", 1L, java.util.Arrays.asList(10L, 12L));
writer.syncWithoutDetaching("roles", 1L, java.util.Arrays.asList(13L));
writer.detach("roles", 1L, java.util.Arrays.asList(10L));
```

## SQL Preview

The query and write APIs expose SQL preview helpers so you can inspect generated SQL before execution.

- `toSql()`
- `toDebugSql()`
- `toSqls()`
- `toDebugSqls()`
- `previewAttachSql(...)`
- `previewDetachSql(...)`
- `previewSyncSql(...)`

This is useful for debugging, logging, and writing tests around generated SQL.

## Publishing

If you want to publish this project as a reusable Maven artifact, the usual flow is:

### 1. Update the version

Change the version in `pom.xml` from `1.0-SNAPSHOT` to a release version:

```xml
<version>1.0.0</version>
```

### 2. Run the full build

```bash
mvn clean test
mvn clean package
```

### 3. Install locally for validation

```bash
mvn clean install
```

### 4. Configure your repository

For a real publish, configure:

- `distributionManagement` in `pom.xml`
- credentials in Maven `settings.xml`
- signing if your target repository requires it

### 5. Deploy

```bash
mvn clean deploy
```

## Notes

- The codebase targets Java 8 language features.
- The package is intentionally small and explicit, so SQL generation stays easy to inspect.
- Test coverage focuses on relation behavior, query generation, and SQL preview output.
