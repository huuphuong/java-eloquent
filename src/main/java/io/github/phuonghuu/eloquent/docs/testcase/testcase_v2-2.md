# RelationKit Testcase Review v2-2

TÃ i liá»‡u nÃ y lÃ  báº£n bá»• sung cuá»‘i cá»§a nhÃ¡nh v2 trÆ°á»›c khi chuyá»ƒn sang ver 3. Má»¥c tiÃªu lÃ  khÃ³a cÃ¡c case thá»±c táº¿ khÃ³ hÆ¡n:

- relation 1-1 khi thiáº¿u dá»¯ liá»‡u
- relation 1-1 khi cÃ³ nhiá»u row vÃ  cáº§n chá»n deterministic báº±ng `orderBy`
- cursor pagination Ä‘i cÃ¹ng eager loading nhiá»u cáº¥p
- offset pagination trÃªn dataset lá»›n hÆ¡n
- `whereJson(...)` vá»›i object cÃ³ cycle

## Test files hiá»‡n cÃ³

- `src/test/java/io/github/phuonghuu/eloquent/relationkit/WithParserTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RowMapperFactoryTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RelationKitIntegrationTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RelationKitPhase2Test.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RelationKitErrorHandlingTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RelationKitAdvancedCoverageTest.java`

## Káº¿t quáº£ kiá»ƒm tra gáº§n nháº¥t

- Command cháº¡y: `mvnw.cmd -q -Dtest=RelationKitIntegrationTest,RelationKitPhase2Test,RelationKitErrorHandlingTest,RelationKitAdvancedCoverageTest,RowMapperFactoryTest,WithParserTest test`
- Káº¿t quáº£: pass

## Coverage má»›i bá»• sung trong v2-2

### 1. `hasOne` - null path

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| User cÃ³ trong relation nhÆ°ng khÃ´ng cÃ³ profile row | `RelationKitAdvancedCoverageTest` | Covered | `with("users.profile")` váº«n attach `null` cho profile thiáº¿u |

### 1b. `hasOne` - duplicate rows / order handling

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Má»™t user cÃ³ nhiá»u profile row | `RelationKitAdvancedCoverageTest` | Covered | `with("users.profile", q -> q.orderBy("id", "DESC"))` chá»n profile cÃ³ id lá»›n hÆ¡n |

### 2. `hasOneThroughPivot` - null path

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Department khÃ´ng cÃ³ pivot row `MANAGER` | `RelationKitAdvancedCoverageTest` | Covered | `with("manager")` attach `null` Ä‘Ãºng nghÄ©a |

### 2b. `hasOneThroughPivot` - duplicate rows / order handling

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Department cÃ³ nhiá»u pivot row `MANAGER` | `RelationKitAdvancedCoverageTest` | Covered | `with("manager", q -> q.orderBy("id", "DESC"))` chá»n user id lá»›n hÆ¡n |

### 3. Nested `with(...)` callback error

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Path sÃ¢u hÆ¡n nhÆ°ng relation con khÃ´ng tá»“n táº¡i | `RelationKitAdvancedCoverageTest` | Covered | `with("users", q -> q.with("profile.ghost"))` fail rÃµ rÃ ng |

### 4. Offset pagination trÃªn dataset lá»›n hÆ¡n

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| 30 departments, page size 10 | `RelationKitAdvancedCoverageTest` | Covered | Check page 1/2/3/4, total, totalPages, hasNext/hasPrevious |

### 5. Cursor pagination + eager loading

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `cursorPaginate()` vá»›i `with("users.profile")` | `RelationKitAdvancedCoverageTest` | Covered | Cursor paging váº«n attach graph á»Ÿ page Ä‘áº§u |
| `cursorPaginate()` vá»›i `with("users.profile")` + `with("manager")` | `RelationKitAdvancedCoverageTest` | Covered | Page sau váº«n cÃ³ nested graph; page 2 cÃ³ Ã­t nháº¥t má»™t item cÃ³ manager |

### 6. `whereJson(...)` invalid object

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Object khÃ´ng serialize Ä‘Æ°á»£c | `RelationKitAdvancedCoverageTest` | Covered | Bá»c thÃ nh `IllegalArgumentException` |
| Map tá»± tham chiáº¿u | `RelationKitAdvancedCoverageTest` | Covered | Cycle cÅ©ng fail fast |

### 7. `whereDate(...)` vá»›i kiá»ƒu khÃ¡c `LocalDate`

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `LocalDateTime` | `RelationKitAdvancedCoverageTest` | Covered | Normalize vá» ngÃ y |
| `ZonedDateTime` | `RelationKitAdvancedCoverageTest` | Covered | Normalize vá» ngÃ y |

## Ã nghÄ©a thá»±c táº¿

- `hasOne` vÃ  `hasOneThroughPivot` khÃ´ng táº¡o object giáº£ khi dá»¯ liá»‡u thiáº¿u.
- Náº¿u cÃ³ dá»¯ liá»‡u 1-1 bá»‹ lá»‡ch vÃ  sinh nhiá»u row, `orderBy` giÃºp test vÃ  runtime deterministic hÆ¡n.
- Cursor pagination khÃ´ng chá»‰ dá»«ng á»Ÿ metadata, mÃ  cÃ²n giá»¯ eager-loaded graph qua cÃ¡c page sau.
- `whereJson(...)` khÃ´ng Ã¢m tháº§m bá» qua lá»—i serialize.

## So vá»›i v2-1

| NhÃ³m | v2-1 | v2-2 |
|---|---:|---:|
| `hasOne` happy path | Covered | Covered |
| `hasOne` null path | Covered | Covered |
| `hasOne` duplicate / order handling | Not covered | Covered |
| `hasOneThroughPivot` happy path | Covered | Covered |
| `hasOneThroughPivot` null path | Covered | Covered |
| `hasOneThroughPivot` duplicate / order handling | Not covered | Covered |
| `whereJson` invalid object | Covered | Covered |
| `whereJson` cycle | Covered | Covered |
| `whereDate` non-LocalDate | Covered | Covered |
| Nested `with(...)` error | Covered | Covered |
| Large dataset offset pagination | Covered | Covered |
| Cursor + deep graph across multiple pages | Not covered | Covered |

## CÃ¡c gap cÃ²n láº¡i sau v2-2

- ChÆ°a cÃ³ test cho object JSON cá»±c sÃ¢u hoáº·c custom serializer.
- ChÆ°a cÃ³ test cho timezone edge cases khÃ¡c mÃºi giá» há»‡ thá»‘ng á»Ÿ `whereDate(...)`.
- ChÆ°a cÃ³ test stress lá»›n hÆ¡n cho cursor pagination kiá»ƒu 50-100 records.
- ChÆ°a cÃ³ test cho ambiguous 1-1 data khi `orderBy` khÃ´ng set rÃµ.
- ChÆ°a cÃ³ test negative riÃªng cho `hasOne` / `hasOneThroughPivot` khi relation callback dÃ¹ng orderBy lá»‡ch kiá»ƒu dá»¯ liá»‡u.

## ÄÃ¡nh giÃ¡ ngáº¯n

V2-2 Ä‘Ã£ khÃ³a Ä‘Æ°á»£c pháº§n quan trá»ng nháº¥t cá»§a edge behavior:

- relation 1-1 thiáº¿u dá»¯ liá»‡u
- relation 1-1 cÃ³ nhiá»u row nhÆ°ng cáº§n chá»n deterministic
- cursor pagination Ä‘i cÃ¹ng nested graph
- JSON cycle fail-fast

Tá»« Ä‘Ã¢y chuyá»ƒn sang ver 3 lÃ  há»£p lÃ½.

