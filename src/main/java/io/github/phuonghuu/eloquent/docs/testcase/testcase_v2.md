# RelationKit Testcase Review v2

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
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RelationKitIntegrationTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RelationKitPhase2Test.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RelationKitErrorHandlingTest.java`

## Káº¿t quáº£ kiá»ƒm tra gáº§n nháº¥t

- Command cháº¡y: `mvnw.cmd -q -Dtest=RelationKitIntegrationTest,RelationKitPhase2Test,RelationKitErrorHandlingTest,RowMapperFactoryTest,WithParserTest test`
- Káº¿t quáº£: pass

## Coverage theo nhÃ³m tÃ­nh nÄƒng

### 1. Parser vÃ  metadata

| TÃ­nh nÄƒng | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `WithParser.parse(...)` | `WithParserTest` | Covered | CÃ³ test merge nested path |
| `RelationRegistry` Ä‘Äƒng kÃ½ há»£p lá»‡ | `RelationKitPhase2Test`, `RelationKitIntegrationTest`, `RelationKitErrorHandlingTest` | Covered | CÃ³ test cáº£ happy path vÃ  missing metadata error |
| `RelationKit.query(...)` vá»›i type chÆ°a Ä‘Äƒng kÃ½ | `RelationKitErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |

### 2. Query root - happy path

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `select(...)` | `RelationKitPhase2Test` | Covered | Projection root |
| `where(...)` | `RelationKitPhase2Test` | Covered | Operator vÃ  equals |
| `orWhere(...)` | `RelationKitPhase2Test` | Covered | CÃ³ test SQL build |
| `whereRaw(...)` | `RelationKitPhase2Test` | Covered | CÃ³ test bind `?` |
| `whereNull` / `whereNotNull` | `RelationKitPhase2Test` | Covered | SQL fragment |
| `whereBetween` | `RelationKitPhase2Test` | Covered | SQL fragment |
| `whereLike` | `RelationKitPhase2Test` | Covered | SQL fragment |
| `whereDate` | `RelationKitPhase2Test` | Covered | SQL fragment |
| `whereJson` | `RelationKitPhase2Test` | Covered | PostgreSQL JSON containment |
| `whereIn(...)` | `RelationKitIntegrationTest`, `RelationKitPhase2Test` | Covered | Batch filter |
| `whereHas(...)` | `RelationKitPhase2Test` | Covered | EXISTS |
| `whereHasIn(...)` | `RelationKitPhase2Test` | Covered | EXISTS + IN |
| `orderBy(...)` | `RelationKitPhase2Test` | Covered | `ASC` / `DESC` |
| `limit(...)` | `RelationKitPhase2Test` | Covered | Root + relation usage |
| `offset(...)` | `RelationKitPhase2Test` | Covered | Root query |
| `page(...)` / `size(...)` | `RelationKitPhase2Test`, `RelationKitIntegrationTest` | Covered | Offset paging |
| `first()` | `RelationKitPhase2Test` | Covered | First row by current state |
| `find(...)` | `RelationKitPhase2Test` | Covered | Primary key lookup |

### 3. Query root - unhappy path

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `orderBy(..., "SIDEWAYS")` | `RelationKitErrorHandlingTest` | Covered | Reject invalid direction |
| `whereRaw(...)` mismatch placeholder/value | `RelationKitErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |
| `paginate(0, size)` / `paginate(page, 0)` | `RelationKitErrorHandlingTest` | Covered | Validate input |
| `cursorPaginate(null)` khi chÆ°a set size | `RelationKitErrorHandlingTest` | Covered | Require size state |
| `cursorPaginate(null, 0)` | `RelationKitErrorHandlingTest` | Covered | Validate input |
| `find(null)` | `RelationKitErrorHandlingTest` | Covered | Tráº£ vá» `null` |
| `first()` trÃªn query rá»—ng | `RelationKitErrorHandlingTest` | Covered | Tráº£ vá» `null` |
| `whereIn(... empty list)` | `RelationKitErrorHandlingTest` | Covered | Tráº£ vá» rá»—ng vÃ  SQL `1 = 0` |

### 4. Relations vÃ  eager loading

| Relation / behavior | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `with("users")` | `RelationKitPhase2Test` | Covered | Projection + limit |
| Nested `with("users.orders")` | `RelationKitIntegrationTest` | Covered | Batch loading nhiá»u cáº¥p |
| `with("orders.items.product")` | `RelationKitIntegrationTest` | Covered | 3 cáº¥p relation |
| `with("roles")` | `RelationKitIntegrationTest` | Covered | Many-to-many |
| `belongsTo` | `RelationKitIntegrationTest` | Covered | OrderItem -> Product |
| `hasMany` | `RelationKitIntegrationTest`, `RelationKitPhase2Test` | Covered | User -> orders, Order -> items |
| `belongsToMany` | `RelationKitIntegrationTest`, `RelationKitPhase2Test` | Covered | User -> roles, Department -> users |
| `limitPerParent(...)` trÃªn belongsToMany | `RelationKitPhase2Test` | Covered | Batch window function |
| `with("ghost")` relation khÃ´ng tá»“n táº¡i | `RelationKitErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |
| `whereHas("ghost")` | `RelationKitErrorHandlingTest` | Covered | NÃ©m `IllegalArgumentException` |
| `limitPerParent(...)` trÃªn relation khÃ´ng há»— trá»£ | `RelationKitErrorHandlingTest` | Covered via preview SQL | Validate á»Ÿ `toSqls()` |

### 5. Pagination

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `paginate(page, size)` | `RelationKitPhase2Test` | Covered | CÃ³ metadata |
| `cursorPaginate(null, size)` | `RelationKitPhase2Test` | Covered | Trang Ä‘áº§u |
| `cursorPaginate(nextCursorId, size)` | `RelationKitPhase2Test` | Covered | Trang tiáº¿p theo |
| `cursorPaginate(... DESC ...)` | `RelationKitPhase2Test` | Covered | Há»— trá»£ chiá»u DESC |
| `cursorPaginate` orderBy nhiá»u cá»™t | `RelationKitErrorHandlingTest` | Covered | Reject vÃ¬ hiá»‡n táº¡i chá»‰ há»— trá»£ 1 cá»™t |
| `cursorPaginate` orderBy khÃ´ng pháº£i primary key | `RelationKitErrorHandlingTest` | Covered | Reject Ä‘á»ƒ trÃ¡nh keyset paging sai |

### 6. SQL preview / debug

| Method | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `toSql()` | `RelationKitPhase2Test` | Covered | SQL root |
| `toDebugSql()` | `RelationKitPhase2Test`, `RelationKitErrorHandlingTest` | Covered | Literal preview |
| `toSqls()` | `RelationKitPhase2Test` | Covered | Root + child queries |
| `toDebugSqls()` | `RelationKitPhase2Test` | Covered | One-line debug SQL |

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

