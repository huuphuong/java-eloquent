# Eloquent Testcase Review v2-1

TÃ i liá»‡u nÃ y ghi láº¡i pháº§n test bá»• sung sau `testcase_v2.md`, táº­p trung vÃ o cÃ¡c coverage cÃ²n thiáº¿u Ä‘Æ°á»£c user yÃªu cáº§u:

- `hasOne`
- `hasOneThroughPivot`
- relation path sÃ¢u hÆ¡n vá»›i `with(...)` callback lá»—i
- `paginate()` trÃªn dataset lá»›n hÆ¡n
- `cursorPaginate()` káº¿t há»£p `with(...)`
- `whereJson(...)` vá»›i object khÃ´ng serialize Ä‘Æ°á»£c
- `whereDate(...)` vá»›i kiá»ƒu dá»¯ liá»‡u khÃ¡c `LocalDate`

## Test files hiá»‡n cÃ³

- `src/test/java/io/github/phuonghuu/eloquent/relationkit/WithParserTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/RowMapperFactoryTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentIntegrationTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentPhase2Test.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentErrorHandlingTest.java`
- `src/test/java/io/github/phuonghuu/eloquent/relationkit/EloquentAdvancedCoverageTest.java`

## Káº¿t quáº£ kiá»ƒm tra gáº§n nháº¥t

- Command cháº¡y: `mvnw.cmd -q -Dtest=EloquentIntegrationTest,EloquentPhase2Test,EloquentErrorHandlingTest,EloquentAdvancedCoverageTest,RowMapperFactoryTest,WithParserTest test`
- Káº¿t quáº£: pass

## Coverage má»›i bá»• sung

### 1. `hasOne`

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Load profile cho má»—i user | `EloquentAdvancedCoverageTest` | Covered | `UserRecord -> UserProfileRecord` qua `hasOne("profile", ...)` |

### 2. `hasOneThroughPivot`

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Load manager duy nháº¥t cho department | `EloquentAdvancedCoverageTest` | Covered | Department láº¥y 1 user tá»« pivot `positions` vá»›i `role_code = MANAGER` |

### 3. Nested `with(...)` callback error

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Path sÃ¢u hÆ¡n nhÆ°ng relation con khÃ´ng tá»“n táº¡i | `EloquentAdvancedCoverageTest` | Covered | `with("users", q -> q.with("profile.ghost"))` nÃ©m lá»—i rÃµ rÃ ng |

### 4. Offset pagination trÃªn dataset lá»›n hÆ¡n

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Multi-page metadata á»•n Ä‘á»‹nh | `EloquentAdvancedCoverageTest` | Covered | Kiá»ƒm tra 5 records, page size 2, cover page 1/2/3 |

### 5. Cursor pagination + eager loading

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `cursorPaginate()` vá»›i `with("users.profile")` | `EloquentAdvancedCoverageTest` | Covered | XÃ¡c nháº­n cursor paging váº«n attach graph Ä‘Ãºng |

### 6. `whereJson(...)` invalid object

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Object khÃ´ng serialize Ä‘Æ°á»£c | `EloquentAdvancedCoverageTest` | Covered | NÃ©m `IllegalArgumentException` khi serializing JSON |

### 7. `whereDate(...)` vá»›i kiá»ƒu khÃ¡c `LocalDate`

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `LocalDateTime` | `EloquentAdvancedCoverageTest` | Covered | Debug SQL váº«n normalize vá» ngÃ y |
| `ZonedDateTime` | `EloquentAdvancedCoverageTest` | Covered | Debug SQL váº«n normalize vá» ngÃ y |

## Ã nghÄ©a cá»§a bá»™ test má»›i

- `hasOne` vÃ  `hasOneThroughPivot` Ä‘Ã£ Ä‘Æ°á»£c khÃ³a báº±ng test happy-path, khÃ´ng chá»‰ cÃ²n tá»“n táº¡i á»Ÿ config.
- Cursor pagination giá» khÃ´ng chá»‰ test pháº§n cursor metadata mÃ  cÃ²n test cÃ¹ng eager loading.
- `whereJson(...)` cÃ³ test tháº¥t báº¡i thá»±c táº¿ khi object khÃ´ng serialize Ä‘Æ°á»£c.
- `whereDate(...)` Ä‘Ã£ Ä‘Æ°á»£c test vá»›i input khÃ¡c `LocalDate`, giÃºp trÃ¡nh giáº£ Ä‘á»‹nh háº¹p trong future refactor.

## CÃ¡c gap cÃ²n láº¡i sau v2-1

- `hasOne` vÃ  `hasOneThroughPivot` má»›i cÃ³ happy-path, chÆ°a cÃ³ negative case riÃªng.
- ChÆ°a cÃ³ test cho `whereJson(...)` vá»›i object nested phá»©c táº¡p hoáº·c self-referencing map.
- ChÆ°a cÃ³ test cho cursor pagination khi káº¿t há»£p relation path sÃ¢u hÆ¡n ngoÃ i `users.profile`.
- ChÆ°a cÃ³ test cho pagination metadata khi sort theo Ä‘iá»u kiá»‡n phá»©c táº¡p hÆ¡n `id ASC`.

## ÄÃ¡nh giÃ¡ ngáº¯n

V2-1 Ä‘Ã£ bá»• sung Ä‘Ãºng pháº§n thiáº¿u nháº¥t cá»§a relation kit: relation biáº¿n thá»ƒ vÃ  unhappy path cho serialization / nested path.
ÄÃ¢y lÃ  ná»n tá»‘t Ä‘á»ƒ sang ver 3 náº¿u cáº§n má»Ÿ rá»™ng pivot sync, aggregate, hoáº·c relation predicates sÃ¢u hÆ¡n.

