# Eloquent Testcase Review v2

TÃ i liá»‡u nÃ y lÃ  báº£n cáº­p nháº­t cá»§a `testcase_ver1.md`, táº­p trung vÃ o hai má»¥c tiÃªu:

1. má»Ÿ rá»™ng coverage cho cÃ¡c nhÃ¡nh lá»—i/unhappy path
2. pháº£n Ã¡nh Ä‘Ãºng cÃ¡c hÃ nh vi tháº­t cá»§a code hiá»‡n táº¡i, Ä‘áº·c biá»‡t vá»›i pagination vÃ  relation validation

## Tráº¡ng thÃ¡i tá»•ng quan

- Query builder, eager loading, SQL preview, parser, row mapper, offset pagination vÃ  cursor pagination Ä‘á»u Ä‘Ã£ cÃ³ test.
- Bá»• sung thÃªm bá»™ test error handling Ä‘á»ƒ kiá»ƒm tra kháº£ nÄƒng chá»‹u lá»—i cá»§a public API.
- Má»™t sá»‘ hÃ nh vi lá»—i Ä‘Æ°á»£c validate á»Ÿ má»©c build SQL / preview SQL, khÃ´ng pháº£i lÃºc runtime `get()`. ÄÃ¢y lÃ  Ä‘iá»ƒm cáº§n nhá»› khi Ä‘á»c test.

## Test files hiá»‡n cÃ³

- `src/test/java/io/github/phuonghuu/eloquent/relationkit/WithParserTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RowMapperFactoryTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentIntegrationTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentPhase2Test.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentErrorHandlingTest.java`

## Káº¿t quáº£ kiá»ƒm tra gáº§n nháº¥t

- Command cháº¡y: `mvnw.cmd -q -Dtest=EloquentIntegrationTest,EloquentPhase2Test,EloquentErrorHandlingTest,RowMapperFactoryTest,WithParserTest test`
- Káº¿t quáº£: pass

## Coverage theo nhÃ³m tÃ­nh nÄƒng

### 1. Parser vÃ  metadata

| TÃ­nh nÄƒng | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `WithParser.parse(...)` | `WithParserTest` | Covered | CÃ³ test merge nested path |
| `RelationRegistry` Ä‘Äƒng kÃ½ há»£p lá»‡ | `EloquentPhase2Test`, `EloquentIntegrationTest`, `EloquentErrorHandlingTest` | Covered | CÃ³ test cáº£ happy path vÃ  missing metadata error |
| `Eloquent.query(...)` vá»›i type chÆ°a Ä‘Äƒng kÃ½ | `EloquentErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |

### 2. Query root - happy path

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `select(...)` | `EloquentPhase2Test` | Covered | Projection root |
| `where(...)` | `EloquentPhase2Test` | Covered | Operator vÃ  equals |
| `orWhere(...)` | `EloquentPhase2Test` | Covered | CÃ³ test SQL build |
| `whereRaw(...)` | `EloquentPhase2Test` | Covered | CÃ³ test bind `?` |
| `whereNull` / `whereNotNull` | `EloquentPhase2Test` | Covered | SQL fragment |
| `whereBetween` | `EloquentPhase2Test` | Covered | SQL fragment |
| `whereLike` | `EloquentPhase2Test` | Covered | SQL fragment |
| `whereDate` | `EloquentPhase2Test` | Covered | SQL fragment |
| `whereJson` | `EloquentPhase2Test` | Covered | PostgreSQL JSON containment |
| `whereIn(...)` | `EloquentIntegrationTest`, `EloquentPhase2Test` | Covered | Batch filter |
| `whereHas(...)` | `EloquentPhase2Test` | Covered | EXISTS |
| `whereHasIn(...)` | `EloquentPhase2Test` | Covered | EXISTS + IN |
| `orderBy(...)` | `EloquentPhase2Test` | Covered | `ASC` / `DESC` |
| `limit(...)` | `EloquentPhase2Test` | Covered | Root + relation usage |
| `offset(...)` | `EloquentPhase2Test` | Covered | Root query |
| `page(...)` / `size(...)` | `EloquentPhase2Test`, `EloquentIntegrationTest` | Covered | Offset paging |
| `first()` | `EloquentPhase2Test` | Covered | First row by current state |
| `find(...)` | `EloquentPhase2Test` | Covered | Primary key lookup |

### 3. Query root - unhappy path

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `orderBy(..., "SIDEWAYS")` | `EloquentErrorHandlingTest` | Covered | Reject invalid direction |
| `whereRaw(...)` mismatch placeholder/value | `EloquentErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |
| `paginate(0, size)` / `paginate(page, 0)` | `EloquentErrorHandlingTest` | Covered | Validate input |
| `cursorPaginate(null)` khi chÆ°a set size | `EloquentErrorHandlingTest` | Covered | Require size state |
| `cursorPaginate(null, 0)` | `EloquentErrorHandlingTest` | Covered | Validate input |
| `find(null)` | `EloquentErrorHandlingTest` | Covered | Tráº£ vá» `null` |
| `first()` trÃªn query rá»—ng | `EloquentErrorHandlingTest` | Covered | Tráº£ vá» `null` |
| `whereIn(... empty list)` | `EloquentErrorHandlingTest` | Covered | Tráº£ vá» rá»—ng vÃ  SQL `1 = 0` |

### 4. Relations vÃ  eager loading

| Relation / behavior | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `with("users")` | `EloquentPhase2Test` | Covered | Projection + limit |
| Nested `with("users.orders")` | `EloquentIntegrationTest` | Covered | Batch loading nhiá»u cáº¥p |
| `with("orders.items.product")` | `EloquentIntegrationTest` | Covered | 3 cáº¥p relation |
| `with("roles")` | `EloquentIntegrationTest` | Covered | Many-to-many |
| `belongsTo` | `EloquentIntegrationTest` | Covered | OrderItem -> Product |
| `hasMany` | `EloquentIntegrationTest`, `EloquentPhase2Test` | Covered | User -> orders, Order -> items |
| `belongsToMany` | `EloquentIntegrationTest`, `EloquentPhase2Test` | Covered | User -> roles, Department -> users |
| `limitPerParent(...)` trÃªn belongsToMany | `EloquentPhase2Test` | Covered | Batch window function |
| `with("ghost")` relation khÃ´ng tá»“n táº¡i | `EloquentErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |
| `whereHas("ghost")` | `EloquentErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |
| `limitPerParent(...)` trÃªn relation khÃ´ng há»— trá»£ | `EloquentErrorHandlingTest` | Covered via preview SQL | Validate á»Ÿ `toSqls()` |

### 5. Pagination

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `paginate(page, size)` | `EloquentPhase2Test` | Covered | CÃ³ metadata |
| `cursorPaginate(null, size)` | `EloquentPhase2Test` | Covered | Trang Ä‘áº§u |
| `cursorPaginate(nextCursorId, size)` | `EloquentPhase2Test` | Covered | Trang tiáº¿p theo |
| `cursorPaginate(... DESC ...)` | `EloquentPhase2Test` | Covered | Há»— trá»£ chiá»u DESC |
| `cursorPaginate` orderBy nhiá»u cá»™t | `EloquentErrorHandlingTest` | Covered | Reject vÃ¬ hiá»‡n táº¡i chá»‰ há»— trá»£ 1 cá»™t |
| `cursorPaginate` orderBy khÃ´ng pháº£i primary key | `EloquentErrorHandlingTest` | Covered | Reject Ä‘á»ƒ trÃ¡nh keyset paging sai |

### 6. SQL preview / debug

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `toSql()` | `EloquentPhase2Test` | Covered | SQL root |
| `toDebugSql()` | `EloquentPhase2Test`, `EloquentErrorHandlingTest` | Covered | Literal preview |
| `toSqls()` | `EloquentPhase2Test` | Covered | Root + child queries |
| `toDebugSqls()` | `EloquentPhase2Test` | Covered | One-line debug SQL |

### 7. Row mapping

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `RowMapperFactory.create(...)` | `RowMapperFactoryTest` | Covered | `Timestamp -> ZonedDateTime` |

## Nhá»¯ng Ä‘iá»u Ä‘Ã£ xÃ¡c nháº­n qua test lá»—i

- Missing registry metadata khÃ´ng bá»‹ nuá»‘t im láº·ng, mÃ  fail sá»›m.
- `whereRaw` khÃ´ng ná»‘i chuá»—i thÃ´; náº¿u placeholder/value khÃ´ng khá»›p thÃ¬ nÃ©m lá»—i ngay.
- `orderBy` chá»‰ nháº­n `ASC` hoáº·c `DESC`.
- Cursor pagination hiá»‡n cháº¥p nháº­n Ä‘Ãºng má»™t cá»™t order vÃ  pháº£i lÃ  primary key.
- Relation path sai sáº½ fail rÃµ rÃ ng, khÃ´ng táº¡o SQL Ã¢m tháº§m.
- `find(null)` vÃ  `first()` trÃªn káº¿t quáº£ rá»—ng Ä‘á»u an toÃ n.

## Gap cÃ²n láº¡i sau v2

### NÃªn bá»• sung tiáº¿p

- Test riÃªng cho `hasOne`
- Test riÃªng cho `hasOneThroughPivot`
- Test relation path sÃ¢u hÆ¡n vá»›i `with(...)` callback lá»—i
- Test `paginate()` trÃªn dataset lá»›n hÆ¡n Ä‘á»ƒ xem metadata á»•n Ä‘á»‹nh khi nhiá»u page
- Test `cursorPaginate()` káº¿t há»£p `with(...)`
- Test input xáº¥u cho `whereJson(...)` khi object khÃ´ng serialize Ä‘Æ°á»£c
- Test `whereDate(...)` vá»›i kiá»ƒu dá»¯ liá»‡u khÃ¡c `LocalDate`

### CÃ³ thá»ƒ cÃ¢n nháº¯c sau

- Test structured log output cho ELK / APM
- Test alias / projection conflict khi select cÃ¹ng má»™t field dÆ°á»›i nhiá»u tÃªn
- Test around `whereHas` nested nhiá»u cáº¥p náº¿u sau nÃ y má»Ÿ rá»™ng API

## ÄÃ¡nh giÃ¡ cuá»‘i

V2 Ä‘Ã£ Ä‘Æ°a bá»™ test tá»« â€œcoverage functionalâ€ sang â€œcoverage functional + failure behaviorâ€.
ÄÃ¢y lÃ  bÆ°á»›c cáº§n thiáº¿t náº¿u relation kit sáº½ Ä‘Æ°á»£c dÃ¹ng rá»™ng trong project, vÃ¬ failure mode giá» Ä‘Ã£ bá»›t mÆ¡ há»“ vÃ  dá»… debug hÆ¡n.

