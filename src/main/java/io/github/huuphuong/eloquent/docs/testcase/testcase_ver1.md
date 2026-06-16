# Eloquent Testcase Review v1

TÃ i liá»‡u nÃ y tá»•ng há»£p láº¡i tÃ¬nh tráº¡ng test hiá»‡n táº¡i cá»§a `relationkit`, dá»±a trÃªn cÃ¡c test case Ä‘ang cÃ³ trong repo vÃ  káº¿t quáº£ cháº¡y test gáº§n nháº¥t.

## Káº¿t luáº­n nhanh

- Bá»™ kit Ä‘Ã£ cÃ³ coverage tá»‘t cho luá»“ng chÃ­nh: query, eager loading, relations, pagination, SQL preview, parser, row mapper.
- CÃ¡c luá»“ng quan trá»ng nháº¥t Ä‘Ã£ cÃ³ test tÃ­ch há»£p cháº¡y Ä‘Æ°á»£c trÃªn H2 mode PostgreSQL.
- Váº«n cÃ²n má»™t sá»‘ gap cáº§n bá»• sung náº¿u muá»‘n dÃ¹ng á»Ÿ má»©c "core library" lÃ¢u dÃ i:
  - `hasOne` chÆ°a cÃ³ test riÃªng
  - `hasOneThroughPivot` chÆ°a cÃ³ test riÃªng
  - cÃ¡c case lá»—i/edge case chÆ°a Ä‘Æ°á»£c khÃ³a Ä‘áº§y Ä‘á»§
  - `cursorPaginate` má»›i test theo primary key, chÆ°a test reject case ngoÃ i primary key
  - `paginate` chÆ°a cÃ³ test cho input invalid
  - `whereRaw` chÆ°a cÃ³ test mismatch placeholder/value

## Test files hiá»‡n cÃ³

- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentIntegrationTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentPhase2Test.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RowMapperFactoryTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/WithParserTest.java`

## Káº¿t quáº£ kiá»ƒm tra gáº§n nháº¥t

- Command cháº¡y: `mvnw.cmd -q -Dtest=EloquentIntegrationTest,EloquentPhase2Test,RowMapperFactoryTest,WithParserTest test`
- Káº¿t quáº£: pass

## Coverage theo nhÃ³m tÃ­nh nÄƒng

### 1. Parser vÃ  metadata

| TÃ­nh nÄƒng | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `WithParser.parse(...)` | `WithParserTest` | Covered | Test merge path `orders.items.product`, `roles.permissions`, `orders.shipments` |
| `RelationRegistry` registration cÆ¡ báº£n | `EloquentPhase2Test`, `EloquentIntegrationTest` | Covered | ÄÃ£ cover `table`, `primaryKey`, `hasMany`, `belongsTo`, `belongsToMany` |
| `Eloquent` entry point | `EloquentPhase2Test`, `EloquentIntegrationTest` | Covered | Query Ä‘Æ°á»£c táº¡o qua `eloquent.query(...)` |

### 2. Query root

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `select(...)` | `EloquentPhase2Test` | Covered | CÃ³ test projection root |
| `where(...)` | `EloquentPhase2Test` | Covered | CÃ³ test operator `<`, `>`, `<=`, `>=` |
| `orWhere(...)` | `EloquentPhase2Test` | Covered | CÃ³ test trong `newPredicateMethodsBuildSafeSql` |
| `whereRaw(...)` | `EloquentPhase2Test` | Covered | CÃ³ test `?` placeholder vÃ  debug SQL |
| `whereNull(...)` / `whereNotNull(...)` | `EloquentPhase2Test` | Covered | CÃ³ test SQL fragment |
| `whereBetween(...)` / `orWhereBetween(...)` | `EloquentPhase2Test` | Covered | CÃ³ test SQL fragment |
| `whereLike(...)` / `orWhereLike(...)` | `EloquentPhase2Test` | Covered | CÃ³ test SQL fragment |
| `whereDate(...)` / `orWhereDate(...)` | `EloquentPhase2Test` | Covered | CÃ³ test vá»›i `LocalDate` |
| `whereJson(...)` / `orWhereJson(...)` | `EloquentPhase2Test` | Covered | CÃ³ test JSON containment PostgreSQL style |
| `whereIn(...)` | `EloquentIntegrationTest`, `EloquentPhase2Test` | Covered | Integration dÃ¹ng Ä‘á»ƒ batch filter, phase2 cÃ³ `whereHasIn` |
| `whereHas(...)` | `EloquentPhase2Test` | Covered | CÃ³ test EXISTS vÃ  lá»c department |
| `whereHasIn(...)` | `EloquentPhase2Test` | Covered | CÃ³ test SQL shape |
| `orderBy(...)` | `EloquentPhase2Test` | Covered | CÃ³ test `ASC`/`DESC` string |
| `limit(...)` | `EloquentPhase2Test` | Covered | CÃ³ test relation query vÃ  pagination path |
| `offset(...)` | `EloquentPhase2Test` | Covered | CÃ³ test `whereHas` query root |
| `page(...)` + `size(...)` | `EloquentPhase2Test`, `EloquentIntegrationTest` | Covered | CÃ³ test offset pagination vÃ  root query paging |
| `first()` | `EloquentPhase2Test` | Covered | CÃ³ test láº¥y row Ä‘áº§u theo order hiá»‡n táº¡i |
| `find(...)` | `EloquentPhase2Test` | Covered | CÃ³ test theo primary key vÃ  retain `with(...)` |

### 3. Eager loading / relations

| Relation / behavior | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `with("users")` | `EloquentPhase2Test` | Covered | CÃ³ test relation callback projection, limit |
| Nested `with("users.orders")` | `EloquentIntegrationTest` | Covered | CÃ³ test nested graph batch load |
| `with("orders.items.product")` | `EloquentIntegrationTest` | Covered | CÃ³ test load 3 cáº¥p relation |
| `with("roles")` | `EloquentIntegrationTest` | Covered | CÃ³ test many-to-many |
| `belongsTo` relation | `EloquentIntegrationTest` | Covered | OrderItem -> Product |
| `hasMany` relation | `EloquentIntegrationTest`, `EloquentPhase2Test` | Covered | User -> orders, Order -> items |
| `belongsToMany` relation | `EloquentIntegrationTest`, `EloquentPhase2Test` | Covered | User -> roles, Department -> users |
| `limitPerParent(...)` | `EloquentPhase2Test` | Covered | CÃ³ test 3 items cho má»—i parent |
| `hasOne` relation | N/A | Not covered | ChÆ°a cÃ³ test riÃªng |
| `hasOneThroughPivot(...)` | N/A | Not covered | ChÆ°a cÃ³ test riÃªng |

### 4. Pagination

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `paginate(page, size)` | `EloquentPhase2Test` | Covered | CÃ³ test total, totalPages, hasNext, hasPrevious |
| `cursorPaginate(null, size)` | `EloquentPhase2Test` | Covered | CÃ³ test trang Ä‘áº§u |
| `cursorPaginate(nextCursorId, size)` | `EloquentPhase2Test` | Covered | CÃ³ test trang tiáº¿p theo |
| `cursorPaginate(... DESC ...)` | `EloquentPhase2Test` | Covered | CÃ³ test order `DESC` theo primary key |
| `cursorPaginate` theo cá»™t khÃ¡c PK | N/A | Not covered | Hiá»‡n implementation Ä‘ang cháº·n trÆ°á»ng há»£p nÃ y |

### 5. SQL preview / debug

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `toSql()` | `EloquentPhase2Test` | Covered | CÃ³ test root SQL |
| `toDebugSql()` | `EloquentPhase2Test` | Covered | CÃ³ test literal render |
| `toSqls()` | `EloquentPhase2Test` | Covered | CÃ³ test root + child queries |
| `toDebugSqls()` | `EloquentPhase2Test` | Covered | CÃ³ test compact output cho ELK |

### 6. Row mapping

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `RowMapperFactory.create(...)` | `RowMapperFactoryTest` | Covered | `Timestamp -> ZonedDateTime` |

## Äiá»ƒm máº¡nh hiá»‡n táº¡i

- CÃ³ integration test xÃ¡c nháº­n eager loading batch khÃ´ng bá»‹ N+1.
- CÃ³ test riÃªng cho SQL preview vÃ  debug SQL.
- CÃ³ test cho conversion timestamp sang `ZonedDateTime`.
- CÃ³ test cho parser nested with path.
- CÃ³ test bao phá»§ pháº§n lá»›n predicate methods Ä‘Ã£ thÃªm.
- CÃ³ test cho cursor pagination cáº£ `ASC` vÃ  `DESC`.

## Gap vÃ  rá»§i ro cÃ²n láº¡i

### Gap má»©c Æ°u tiÃªn cao

- ChÆ°a cÃ³ test riÃªng cho `hasOne`.
- ChÆ°a cÃ³ test riÃªng cho `hasOneThroughPivot`.
- ChÆ°a cÃ³ test cho `find(null)` tráº£ vá» `null`.
- ChÆ°a cÃ³ test cho `first()` trÃªn query rá»—ng.
- ChÆ°a cÃ³ test cho `paginate()` khi `page < 1` hoáº·c `size < 1`.
- ChÆ°a cÃ³ test cho `cursorPaginate()` khi `orderBy` khÃ´ng pháº£i primary key.

### Gap má»©c Æ°u tiÃªn trung bÃ¬nh

- ChÆ°a cÃ³ test cho `whereRaw(...)` vá»›i mismatch sá»‘ lÆ°á»£ng `?` vÃ  values.
- ChÆ°a cÃ³ test cho `whereIn(...)` vá»›i collection rá»—ng á»Ÿ root query.
- ChÆ°a cÃ³ test cho `whereJson(...)` vá»›i string JSON phá»©c táº¡p hoáº·c nested object.
- ChÆ°a cÃ³ test cho alias/duplicate column selection trong `select(...)` á»Ÿ nhiá»u relation level.

### Gap má»©c Æ°u tiÃªn tháº¥p

- ChÆ°a cÃ³ test trá»±c tiáº¿p cho `toSql()` cá»§a tá»«ng relation kind riÃªng láº» ngoÃ i integration.
- ChÆ°a cÃ³ test cho log output format á»Ÿ ELK ngoÃ i kiá»ƒm tra `compactSql(...)`.
- ChÆ°a cÃ³ test cho `with(...)` callback khÃ´ng truyá»n callback.

## ÄÃ¡nh giÃ¡ hiá»‡n táº¡i

- Náº¿u má»¥c tiÃªu lÃ  dÃ¹ng ná»™i bá»™ cho project backend, bá»™ test hiá»‡n táº¡i Ä‘Ã£ Ä‘á»§ tá»‘t Ä‘á»ƒ báº¯t lá»—i chÃ­nh á»Ÿ query builder vÃ  eager loader.
- Náº¿u má»¥c tiÃªu lÃ  phÃ¡t hÃ nh library dÃ¹ng rá»™ng, nÃªn bá»• sung thÃªm test theo hÆ°á»›ng "negative cases" vÃ  relation variant coverage.

## Äá» xuáº¥t cho ver2

- ThÃªm test `hasOne`
- ThÃªm test `hasOneThroughPivot`
- ThÃªm test invalid inputs cho `paginate`/`cursorPaginate`
- ThÃªm test edge cases cho `whereRaw`, `whereIn`, `find`, `first`
- ThÃªm test á»•n Ä‘á»‹nh cho `with` nested sÃ¢u hÆ¡n náº¿u cÃ³ relation má»›i

## Danh sÃ¡ch test hiá»‡n há»¯u

- `WithParserTest`
- `RowMapperFactoryTest`
- `EloquentPhase2Test`
- `EloquentIntegrationTest`

## Ghi chÃº

TÃ i liá»‡u nÃ y lÃ  báº£n tá»•ng há»£p `ver1`, Æ°u tiÃªn pháº£n Ã¡nh tÃ¬nh tráº¡ng test thá»±c táº¿ Ä‘ang cÃ³ trong repo, khÃ´ng pháº£i Ä‘áº·c táº£ lÃ½ thuyáº¿t.

