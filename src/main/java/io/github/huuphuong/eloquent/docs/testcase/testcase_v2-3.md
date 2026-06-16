# Eloquent Testcase Review v2-3

ÄÃ¢y lÃ  báº£n chá»‘t cuá»‘i cá»§a nhÃ¡nh v2 trÆ°á»›c khi chuyá»ƒn sang ver 3. CÃ¡c testcase trong báº£n nÃ y táº­p trung vÃ o edge cases cÃ²n láº¡i mÃ  bá»™ kit cáº§n chá»‹u Ä‘Æ°á»£c trong mÃ´i trÆ°á»ng thá»±c táº¿:

- JSON deep structure vÃ  custom serializer
- timezone boundary cho `whereDate(...)`
- stress cursor pagination vá»›i dataset 50-100 records
- ambiguous 1-1 data khi khÃ´ng set `orderBy`
- negative case cho `hasOne` / `hasOneThroughPivot` khi relation callback dÃ¹ng `orderBy` sai cá»™t

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

## Coverage má»›i bá»• sung trong v2-3

### 1. `whereJson(...)` - deep nested structure vÃ  custom serializer

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Deep nested map/list + custom serializer | `EloquentAdvancedCoverageTest` | Covered | XÃ¡c nháº­n JSON deep path vÃ  serializer annotation Ä‘á»u Ä‘Æ°á»£c ObjectMapper xá»­ lÃ½ |

### 2. `whereDate(...)` - timezone boundary

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| UTC gáº§n ná»­a Ä‘Ãªm | `EloquentAdvancedCoverageTest` | Covered | Normalize vá» Ä‘Ãºng date theo timezone cá»§a input |
| `+08:00` gáº§n Ä‘áº§u ngÃ y | `EloquentAdvancedCoverageTest` | Covered | Normalize vá» Ä‘Ãºng date khÃ¡c vá»›i UTC |

### 3. Cursor pagination - stress dataset lá»›n

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| 100 departments, page size 25 | `EloquentAdvancedCoverageTest` | Covered | Loop háº¿t 4 pages, tá»•ng item duyá»‡t = 100 |

### 4. Ambiguous 1-1 data khi khÃ´ng set `orderBy`

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| Má»™t user cÃ³ nhiá»u profile row nhÆ°ng query khÃ´ng set order | `EloquentAdvancedCoverageTest` | Covered | Test chá»‰ khÃ³a ráº±ng káº¿t quáº£ lÃ  má»™t trong cÃ¡c row há»£p lá»‡, khÃ´ng bá»‹ null |

### 5. Negative case cho relation 1-1 khi `orderBy` sai cá»™t

| Case | Test file | Tráº¡ng thÃ¡i | Ghi chÃº |
|---|---|---:|---|
| `hasOne` callback orderBy trÃªn cá»™t khÃ´ng tá»“n táº¡i | `EloquentAdvancedCoverageTest` | Covered | `DataAccessException` |
| `hasOneThroughPivot` callback orderBy trÃªn cá»™t khÃ´ng tá»“n táº¡i | `EloquentAdvancedCoverageTest` | Covered | `DataAccessException` |

## Ã nghÄ©a thá»±c táº¿

- JSON serialization cá»§a kit khÃ´ng chá»‰ á»•n vá»›i map Ä‘Æ¡n giáº£n, mÃ  cÃ²n chá»‹u Ä‘Æ°á»£c object phá»©c táº¡p vÃ  custom serializer.
- `whereDate(...)` khÃ´ng bá»‹ phá»¥ thuá»™c vÃ o má»™t mÃºi giá» cá»‘ Ä‘á»‹nh theo JVM, mÃ  pháº£n Ã¡nh Ä‘Ãºng local date cá»§a input.
- Cursor pagination khÃ´ng chá»‰ cháº¡y Ä‘Ãºng vÃ i trang Ä‘áº§u, mÃ  váº«n á»•n khi dá»¯ liá»‡u lá»›n hÆ¡n.
- Relation 1-1 Ä‘Æ°á»£c test trong 2 tÃ¬nh huá»‘ng khÃ³ nháº¥t:
  - thiáº¿u dá»¯ liá»‡u thÃ¬ pháº£i tráº£ `null`
  - dÆ° dá»¯ liá»‡u thÃ¬ pháº£i deterministic hoáº·c Ã­t nháº¥t khÃ´ng vá»¡

## So vá»›i v2-2

| NhÃ³m | v2-2 | v2-3 |
|---|---:|---:|
| `whereJson` invalid object / cycle | Covered | Covered |
| `whereJson` deep structure + custom serializer | Not covered | Covered |
| `whereDate` non-LocalDate | Covered | Covered |
| `whereDate` timezone boundary | Not covered | Covered |
| Large dataset offset pagination | Covered | Covered |
| Cursor stress 50-100 records | Not covered | Covered |
| `hasOne` null / duplicate handling | Covered | Covered |
| `hasOne` ambiguous without orderBy | Not covered | Covered |
| `hasOneThroughPivot` null / duplicate handling | Covered | Covered |
| `hasOneThroughPivot` ambiguous / invalid orderBy | Not covered | Covered |

## CÃ¡c gap cÃ²n láº¡i sau v2-3

- ChÆ°a cÃ³ test cho object JSON cá»±c lá»›n hoáº·c circular graph phá»©c táº¡p hÆ¡n 1 level map tá»± tham chiáº¿u.
- ChÆ°a cÃ³ test stress lá»›n hÆ¡n 100 records cho cursor pagination.
- ChÆ°a cÃ³ test timezone edge cases theo DST / mÃºi giá» cÃ³ daylight saving.
- ChÆ°a cÃ³ test negative cho `hasOne` / `hasOneThroughPivot` khi DB schema thá»±c sá»± vi pháº¡m nhiá»u invariant cÃ¹ng lÃºc.
- ChÆ°a cÃ³ test for `whereDate(...)` khi dÃ¹ng `OffsetDateTime` náº¿u sau nÃ y kit má»Ÿ support.

## ÄÃ¡nh giÃ¡ cuá»‘i nhÃ¡nh v2

V2-3 lÃ  má»©c coverage Ä‘á»§ máº¡nh Ä‘á»ƒ chuyá»ƒn sang ver 3.

Äiá»ƒm Ä‘Ã¡ng giÃ¡ nháº¥t cá»§a nhÃ¡nh nÃ y:

- khÃ´ng chá»‰ cover happy-path
- Ä‘Ã£ cover nhá»¯ng lá»—i thá»±c táº¿ thÆ°á»ng xuáº¥t hiá»‡n khi dÃ¹ng relation kit trong service tháº­t
- Ä‘áº·c biá»‡t lÃ  tÃ¬nh huá»‘ng ambiguity, serialization phá»©c táº¡p, timezone boundary vÃ  cursor stress

Tá»« Ä‘Ã¢y sang ver 3 lÃ  há»£p lÃ½, vÃ¬ nhá»¯ng pháº§n cÃ²n láº¡i nÃªn lÃ  má»Ÿ rá»™ng feature, khÃ´ng pháº£i vÃ¡ coverage cÆ¡ báº£n ná»¯a.

