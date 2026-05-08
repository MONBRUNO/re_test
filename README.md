# Naengbuhae_Team_backend

> 스마트 냉장고 / 식재료 관리 앱 **냉부해**의 백엔드 서버.
> 사용자가 보유한 식재료를 기반으로 만들 수 있는 레시피를 추천하고,
> 신체정보에 맞춘 일일 권장 칼로리·식단을 제안한다.

---

## 🛠 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어 | Java 17 (Amazon Corretto) |
| 프레임워크 | Spring Boot 3.2.4 |
| 보안 | Spring Security · JWT (jjwt 0.12.5) · OAuth2 Client (카카오) |
| 데이터 | Spring Data JPA · Hibernate 6 · PostgreSQL (Supabase) |
| 도구 | Lombok · Springdoc OpenAPI(Swagger) · Gradle |

---

## ✨ 주요 기능

### 1. 인증 / 회원
- **일반 회원가입 / 로그인** — JWT 기반 세션리스 인증
- **카카오 소셜 로그인** — Spring Security OAuth2 Client
  - 같은 이메일이면 기존 LOCAL 계정에 자동 연결 (B-1 정책)
  - 카카오 일반 앱이 이메일 권한을 못 받는 경우 `kakao_{providerId}@kakao.local` placeholder 자동 생성
- **프로필 조회·수정** (`/user/me`) — 신체정보 변경 시 권장 칼로리 자동 재계산
- **회원가입 입력 검증** — 아이디 영문/숫자 조합, 비밀번호(영소문자+숫자+특수문자), 성별·활동량·식단목표 enum 정렬

### 2. 식재료 (Ingredient)
- 식재료 등록·조회·수정·삭제 (개인 격리)
- **유통기한 임박 알림** — 기본 3일 이내, `days` 파라미터로 조정
  - 상태 분류: `safe` / `warning`(≤3일) / `danger`(만료/지남)

### 3. 레시피 (Recipe)
- 레시피 CRUD (관리자 또는 시스템 시드 사용자가 등록)
- **보유 식재료 기반 추천** (`/api/recipes/recommendations`) — 매칭률 + 보유/누락 식재료를 함께 반환
- 카테고리·난이도·영양정보·조리 단계 구조화 (프론트 spec 정렬)

### 4. 장보기 (Shopping List)
- 장보기 항목 CRUD
- **구매 완료 → 냉장고 자동 이관** (`POST /api/shopping-list/move-to-fridge`)

### 5. 일일 권장 칼로리
- Mifflin-St Jeor 공식 기반 BMR 계산 → 활동량 가중치 → 식단 목표 보정
- 회원가입 시 계산하여 저장, 신체정보 수정 시 재계산

### 6. 관리자
- 전체 유저·레시피 목록, 시스템 통계, 레시피 강제 삭제 (`@PreAuthorize("hasRole('ADMIN')")`)

---

## 📡 API 명세 (요약)

> 인증이 필요한 엔드포인트는 `Authorization: Bearer {JWT}` 헤더 필요.

### 인증
| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/user/signup` | 회원가입 | ❌ |
| POST | `/user/login` | 로그인 (JWT 발급) | ❌ |
| GET | `/oauth2/authorization/kakao` | 카카오 OAuth 시작 | ❌ |
| GET | `/login/oauth2/code/kakao` | 카카오 콜백 (자동, 프론트로 redirect with token) | ❌ |

### 유저
| Method | Path | 설명 |
|---|---|---|
| GET | `/user/me` | 내 프로필 조회 |
| PUT | `/user/me` | 내 프로필 수정 (권장 칼로리 재계산) |

### 식재료
| Method | Path | 설명 |
|---|---|---|
| GET | `/api/ingredients` | 내 식재료 목록 |
| POST | `/api/ingredients` | 식재료 추가 |
| PUT | `/api/ingredients/{id}` | 식재료 수정 |
| DELETE | `/api/ingredients/{id}` | 식재료 삭제 |
| GET | `/api/ingredients/expiring?days=3` | 유통기한 임박 |

### 레시피
| Method | Path | 설명 |
|---|---|---|
| GET | `/api/recipes` | 레시피 목록 |
| GET | `/api/recipes/recommendations` | 보유 식재료 기반 추천 |
| POST | `/api/recipes` | 레시피 추가 |
| PUT | `/api/recipes/{id}` | 레시피 수정 |
| DELETE | `/api/recipes/{id}` | 레시피 삭제 |

### 장보기
| Method | Path | 설명 |
|---|---|---|
| GET | `/api/shopping-list` | 목록 |
| POST | `/api/shopping-list` | 항목 추가 |
| DELETE | `/api/shopping-list/{id}` | 항목 삭제 |
| POST | `/api/shopping-list/move-to-fridge` | 구매 완료 → 냉장고 이관 |

### 관리자 (ADMIN 권한)
| Method | Path | 설명 |
|---|---|---|
| GET | `/admin/users` | 전체 유저 |
| GET | `/admin/recipes` | 전체 레시피 |
| DELETE | `/admin/recipes/{recipeId}` | 레시피 강제 삭제 |
| GET | `/admin/stats` | 시스템 통계 |

> Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## 📁 프로젝트 구조

```
src/main/java/com/example/Naengbuhae/
├── NaengbuhaeApplication.java        # 엔트리포인트
├── config/                            # 설정 (보안 / OAuth / JWT / CORS / 시드)
│   ├── SecurityConfig.java
│   ├── PasswordEncoderConfig.java     # 빈 순환 참조 방지를 위해 분리
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomOAuth2UserService.java   # 제공자별 사용자 정보 통일 + 자동 연결
│   ├── OAuth2SuccessHandler.java      # JWT 발급 + 프론트 redirect
│   ├── OAuth2UserInfo.java            # kakao/naver/google 응답 파서
│   └── RecipeSeeder.java              # 최초 부팅 시 시드 레시피 + system 사용자
├── controller/                        # REST 엔드포인트
│   ├── IngredientController.java
│   ├── RecipeController.java
│   └── ShoppingListController.java
├── domain/                            # JPA 엔티티
│   ├── Ingredient.java + IngredientStorageType / IngredientCategory
│   ├── Recipe.java + RecipeIngredient + Difficulty + Nutrition
│   └── ShoppingItem.java
├── dto/                               # 요청 / 응답 DTO
├── exception/
│   └── GlobalExceptionHandler.java    # @Valid 실패 등을 ApiResponse(JSON)로 통일
├── repository/                        # Spring Data JPA
├── service/                           # 비즈니스 로직 (레시피 매칭 등)
├── user/                              # 유저 / OAuth / 관리자 도메인
│   ├── User.java + UserRole + OAuthProvider
│   ├── UserController + UserService + UserRepository
│   ├── AdminController.java           # @PreAuthorize 기반 권한 분리
│   └── DTO들 (SignupRequest, ProfileUpdateRequest, UserResponseDto, ApiResponse 등)
└── util/
    └── CalorieCalculator.java         # Mifflin-St Jeor BMR/TDEE 계산
```

---

## 🔐 환경 변수 (.env)

프로젝트 루트의 `.env`에 다음 값을 채운다 (`.env`는 `.gitignore`에 등록됨):

```dotenv
# DB (Supabase)
DB_URL=jdbc:postgresql://...
DB_USERNAME=postgres.xxxxxx
DB_PASSWORD=실제_비밀번호

# CORS 허용 origin (와일드카드 패턴)
ALLOWED_ORIGINS=http://localhost:*,http://127.0.0.1:*

# JWT 비밀키
JWT_SECRET=충분히_긴_랜덤_문자열_64자_이상_권장

# 카카오 OAuth (개발자 콘솔에서 발급)
KAKAO_CLIENT_ID=발급받은_REST_API_키
KAKAO_CLIENT_SECRET=발급받은_시크릿_코드
OAUTH2_FRONTEND_REDIRECT=http://localhost:5173/oauth/callback
```

---

## 🚀 실행

```bash
# 의존성 설치 + 빌드
./gradlew build

# 서버 기동
./gradlew bootRun
# 또는 IntelliJ에서 NaengbuhaeApplication 직접 실행
```

기본 포트: **8080**

---

## 📋 카카오 OAuth 설정 (개발자 콘솔)

1. https://developers.kakao.com/ → 앱 생성
2. **앱 → 플랫폼 키 → 카카오 로그인 리다이렉트 URI** 등록
   - `http://localhost:8080/login/oauth2/code/kakao`
3. **앱 → 일반 → 웹 도메인** 등록 (`http://localhost:8080`)
4. **카카오 로그인 → 일반 → 사용 설정 ON**
5. **카카오 로그인 → 동의항목 → 닉네임 필수 동의**
   - (이메일은 비즈 앱 전환 전엔 권한 받을 수 없음 — placeholder로 자동 처리)
6. **앱 → 고급 → 클라이언트 시크릿 → 코드 발급 + 활성화**
7. 발급된 REST API 키와 시크릿을 `.env`에 추가

---

## 🗄 DB 스키마 마이그레이션

JPA `ddl-auto=update`이지만 컬럼 추가/제약 변경은 SQL로 직접 실행해야 한다.

### OAuth 도입 시 (Supabase SQL Editor)
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);
UPDATE users SET provider = 'LOCAL' WHERE provider IS NULL;
ALTER TABLE users ALTER COLUMN provider SET NOT NULL;
```

---

## 📝 변경 이력 (시간 순)

> 이 섹션은 시간순 작업 로그입니다. 새 작업은 **맨 아래에 새 엔트리로 append** (위 섹션은 현재 상태 기준이므로 inline 갱신 OK).

### 2026-05-06 — 구글 / 네이버 OAuth 추가 + 네이버 prefill

**커밋**: `0552a7d feat: 구글/네이버 OAuth 로그인 추가 + 네이버 prefill (성별·생년월일)`

#### 코드 변경

- **구글 OAuth 추가** (`application.properties`)
  - Spring Security 내장 provider 사용 → URL 자동 처리
  - `scope=profile,email` — 기본 프로필과 이메일만
- **네이버 OAuth 추가** (`application.properties`)
  - 내장 provider 없어서 URL 직접 지정 (nid.naver.com / openapi.naver.com)
  - `scope=name,email,gender,birthday,birthyear`
  - `user-name-attribute=response` (네이버는 응답이 `{response: {...}}`로 한 번 감싸짐)
- **네이버 회원정보 자동 prefill**
  - `OAuth2UserInfo`에 `gender` (M/F→남/여 매핑) + `birthDate` (`birthyear`+`birthday` 조합) 필드 추가
  - `User.prefillFromOAuth(gender, birthDate)` — 비어있을 때만 채우는 안전한 setter
  - `CustomOAuth2UserService.createNewUser()`에서 신규 사용자 저장 직후 prefill 호출
  - **결과**: 네이버 사용자는 이름/이메일/성별/생년월일 4개 항목이 자동 입력 → 사용자는 키/몸무게/활동량/식단 4개만 추가 입력

#### `.env`에 추가 필요한 환경 변수

```dotenv
# 구글 OAuth (console.cloud.google.com)
GOOGLE_CLIENT_ID=...apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-...

# 네이버 OAuth (developers.naver.com)
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
```

#### 구글 콘솔 설정 — https://console.cloud.google.com/

1. 프로젝트 생성 → **API 및 서비스 → Google 인증 플랫폼**에서 시작 마법사 진행 (앱 이름/사용자 이메일/외부 사용자 유형)
2. 좌측 **클라이언트** → **+ 클라이언트 만들기** → 웹 애플리케이션
   - 승인된 JavaScript 원본: `http://localhost:8080`, `http://localhost:5173`
   - **승인된 리디렉션 URI**: `http://localhost:8080/login/oauth2/code/google`
3. ⚠️ 생성 직후 표시되는 **클라이언트 보안 비밀번호는 한 번만 보임** — 즉시 복사 또는 JSON 다운로드
4. 좌측 **대상** → 테스트 사용자에 본인 이메일 추가 (테스트 모드에서만 본인 외 차단됨)

#### 네이버 콘솔 설정 — https://developers.naver.com/

1. 상단 **Application → 애플리케이션 등록**
2. **사용 API: 네이버 로그인** 체크 → **제공 정보 선택**:
   - ☑ 회원이름 / 이메일 / **성별** / **생일** / **출생연도**
   - (성별·생일·출생연도까지 받아두면 서버가 자동으로 회원 정보에 prefill)
3. **로그인 오픈 API 서비스 환경 → PC웹**:
   - 서비스 URL: `http://localhost:8080`
   - **Callback URL**: `http://localhost:8080/login/oauth2/code/naver`
4. 등록 완료 후 앱 상세 페이지에서 Client ID / Secret 확인 (네이버는 언제든 다시 볼 수 있음)

#### 다음 후보

- 모바일 앱 OAuth 흐름 (네이티브 SDK 연동 시 별도 클라이언트 ID 발급 필요)

---

### 2026-05-07 — 인증 보강 (회원 탈퇴 + Refresh Token + 로그아웃)

#### 1) 회원 탈퇴 — `DELETE /user/me`

- `UserController.deleteMe(Principal)` 추가 — 본인 계정만 삭제
- `UserService.deleteMyAccount(username)` — 사용자가 만든 모든 데이터를 cascade로 정리한 뒤 user row 삭제
  - `IngredientRepository.deleteByUser(user)`
  - `RecipeRepository.deleteByUser(user)`
  - `ShoppingItemRepository.deleteByUser(user)`
  - `RefreshTokenService.revokeAllForUser(user)` — 발급된 refresh token row까지 함께 폐기
- 식재료/레시피/장보기 Repository에 `deleteByUser(User user)` 메서드를 추가 (Spring Data JPA가 자동 구현)

#### 2) Refresh Token 도입

`access token`이 30분으로 짧기 때문에, 사용자가 매번 다시 로그인하지 않도록 `refresh token`을 같이 발급한다.
저장 위치는 **DB 테이블** — 서버 재시작에도 보존되고, 로그아웃·탈퇴 시 row를 지워서 즉시 무효화할 수 있다.

##### 새 엔티티 / 저장소 / 서비스
- `RefreshToken` (`refresh_tokens` 테이블)
  - 컬럼: `id`, `token (unique)`, `user_id`, `expires_at`
  - hibernate `ddl-auto=update`로 자동 생성됨
- `RefreshTokenRepository` — `findByToken`, `deleteByToken`, `deleteByUser`
- `RefreshTokenService`
  - `issue(user)` — UUID 형태의 토큰 생성 후 DB 저장
  - `refresh(refreshToken)` — 기존 row 검증/폐기 후 새 access + 새 refresh 동시 발급 (rotation)
  - `revoke(refreshToken)` — 단일 row 삭제 (로그아웃)
  - `revokeAllForUser(user)` — 사용자 모든 row 일괄 삭제 (탈퇴)

##### 새 엔드포인트 (모두 `permitAll` — access 만료 후에도 호출 가능해야 함)

| 메서드 | 경로 | 요청 body | 응답 |
|---|---|---|---|
| POST | `/user/token/refresh` | `{"refreshToken":"..."}` | `{"token":"...","refreshToken":"..."}` |
| POST | `/user/logout` | `{"refreshToken":"..."}` (선택) | `ApiResponse(true, "로그아웃 되었습니다.")` |

`SecurityConfig`의 `permitAll` 매처에 `/user/token/refresh`, `/user/logout` 추가.

#### 3) 로그인 응답 / OAuth 콜백 확장

- `LoginResponse`에 `refreshToken` 필드 추가 → `/user/login` 응답이 `{success, message, token, refreshToken}` 형태가 됨
- `OAuth2SuccessHandler`에서 access + refresh를 모두 발급한 뒤, 프론트 콜백 URL에 `?token=...&refreshToken=...&needsAdditionalInfo=...` 형태로 redirect

#### `application.properties`에 추가된 설정

```properties
# refresh token 만료 (기본 14일 = 1209600000ms). 짧게 두면 테스트 편리.
app.jwt.refresh-token-expiration-ms=${REFRESH_TOKEN_EXPIRATION_MS:1209600000}
```

`.env`에 별도 키 없이도 동작 (default 값으로 14일).

#### 다음 후보

- ~~access token 만료시간도 properties로 분리 (현재 `JwtUtil.EXPIRATION` 하드코딩 30분)~~ → 분리 완료 (`app.jwt.access-token-expiration-ms`)
- OAuth provider unlink — 탈퇴 시 카카오/구글/네이버에 토큰 unlink 호출 (현재는 자체 DB row만 삭제 → 동일 provider로 다시 로그인하면 신규 사용자처럼 다시 가입됨)
- 만료된 refresh token row 정리 스케줄러

---

### 2026-05-07 (2) — 인증 흐름 후속 수정 + 식재료 분류/보관방법 enum화

#### 1) 인증 실패 시 401 JSON 응답으로 통일

`oauth2Login()` 활성화 때문에 인증되지 않은 요청에 대해 Spring Security가 OAuth 로그인 URL로 **302 redirect**시키던 default 동작이 살아있었음. 이 때문에 access token 만료 후 보호된 API 호출 시 프론트 wrapper가 401을 못 받아 자동 refresh가 동작하지 않던 버그 발견.

`SecurityConfig.exceptionHandling`으로 `authenticationEntryPoint`를 명시:

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
    })
)
```

OAuth 시작/콜백 경로는 어차피 `permitAll`이라 영향 없음. 이후 통합 테스트(access 1분/refresh 3분)로 자동 refresh 흐름이 정상 동작함을 확인.

#### 2) access token 만료시간 properties 분리

`JwtUtil.EXPIRATION` 하드코딩(30분)을 `@Value("${app.jwt.access-token-expiration-ms:1800000}")`로 변경. `.env`의 `JWT_ACCESS_TOKEN_EXPIRATION_MS`로 override 가능. default는 그대로 30분이라 운영 영향 없음.

#### 3) `Ingredient.category` / `storage`를 enum화

##### 신규 enum
- `Category` — VEGETABLE / MEAT / DAIRY / GRAIN / SEAFOOD / FRUIT / ETC
- `Storage` — REFRIGERATED / FROZEN / ROOM

```java
public enum Category {
    VEGETABLE("채소"), MEAT("육류"), DAIRY("유제품"),
    GRAIN("곡물"), SEAFOOD("해산물"), FRUIT("과일"), ETC("기타");

    private final String label;

    @JsonValue
    public String getLabel() { return label; }

    @JsonCreator
    public static Category fromLabel(String value) {
        // 한글 라벨 또는 영어 enum 이름 둘 다 허용
    }
}
```

- DB 저장: `@Enumerated(EnumType.STRING)` → enum 이름(영어)으로 저장
- API 직렬화: `@JsonValue`로 한글 라벨로 응답 → 프론트 변경 0
- API 역직렬화: `@JsonCreator`가 한글 라벨 / 영어 이름 둘 다 받음

##### 기타 영향
- `Ingredient.category/storage` 필드 타입을 enum으로 변경
- `IngredientRequestDto / ResponseDto / ExpiringIngredientResponseDto` 타입 동기화
- `ShoppingItemService.moveCheckedItemsToFridge`의 하드코딩(`"미분류"`, `"냉장"`)을 `Category.ETC`, `Storage.REFRIGERATED`로 교체

##### ⚠️ DB 마이그레이션 (서버 재시작 *전* 실행)

기존 데이터가 한글 또는 영어 소문자로 저장돼있어서 enum 이름(대문자)과 매칭이 안 됨. Supabase SQL Editor에서 한 번 실행:

```sql
-- 한글 → 영어 대문자
UPDATE ingredient SET category = 'VEGETABLE' WHERE category = '채소';
UPDATE ingredient SET category = 'MEAT'      WHERE category = '육류';
UPDATE ingredient SET category = 'DAIRY'     WHERE category = '유제품';
UPDATE ingredient SET category = 'GRAIN'     WHERE category = '곡물';
UPDATE ingredient SET category = 'SEAFOOD'   WHERE category = '해산물';
UPDATE ingredient SET category = 'FRUIT'     WHERE category = '과일';
UPDATE ingredient SET category = 'ETC'       WHERE category IN ('기타', '미분류');

UPDATE ingredient SET storage = 'REFRIGERATED' WHERE storage = '냉장';
UPDATE ingredient SET storage = 'FROZEN'       WHERE storage = '냉동';
UPDATE ingredient SET storage = 'ROOM'         WHERE storage = '실온';

-- 영어 소문자 잔재(예: 'dairy') → 대문자
UPDATE ingredient SET category = UPPER(category)
WHERE category IN ('vegetable','meat','dairy','grain','seafood','fruit','etc');

UPDATE ingredient SET storage = UPPER(storage)
WHERE storage IN ('refrigerated','frozen','room');
```

#### 다음 후보

- ~~`Recipe.category`도 같은 방식으로 enum화 (이번 작업은 `Ingredient` scope만)~~ → 완료 (아래 entry 참고)
- OAuth provider unlink + 만료된 refresh token row 정리 스케줄러 (이전 entry에서 이월)

---

### 2026-05-07 (3) — `Recipe.category` enum화

식재료 enum 정리의 자연스러운 확장. 같은 패턴 적용.

#### 신규 enum
- `RecipeCategory` — MAIN("밥/면") / SIDE("반찬") / SALAD("샐러드") / SNACK("간식") / DRINK("음료") / ETC("기타")
- 시드(`RecipeSeeder`)의 5개 카테고리 + ETC fallback

#### 영향 범위
- `Recipe.category` 필드 타입을 `String` → `RecipeCategory`로 변경 + `@Enumerated(EnumType.STRING)`
- `RecipeRequestDto / ResponseDto` 동기화 (`@JsonValue`로 응답은 한글, `@JsonCreator`로 한글/영어 둘 다 입력 허용)
- `RecipeSeeder.recipe()` 헬퍼 시그니처가 `String` → `RecipeCategory`로 변경되어 8개 시드 호출처 모두 enum 상수로 교체

#### ⚠️ DB 마이그레이션 (서버 재시작 *전* 실행)

```sql
UPDATE recipe SET category = 'MAIN'  WHERE category = '밥/면';
UPDATE recipe SET category = 'SIDE'  WHERE category = '반찬';
UPDATE recipe SET category = 'SALAD' WHERE category = '샐러드';
UPDATE recipe SET category = 'SNACK' WHERE category = '간식';
UPDATE recipe SET category = 'DRINK' WHERE category = '음료';
UPDATE recipe SET category = 'ETC'   WHERE category = '기타';
```

#### 검증 상태
- 컴파일 통과, 시드 8개 enum 이름으로 정상 저장 확인
- 다만 프론트 `recipeStore.fetchRecipes`는 `/api/recipes`(본인 등록만)를 호출하므로 system 계정 소유 시드는 화면에 노출 안 됨 — enum 동작 자체와는 무관한 기존 동작

#### 다음 후보

- 프론트 `recipeStore`를 `/api/recipes/recommendations` 호출로 변경(응답 형식이 `RecipeMatchResponseDto`라 `normalizeRecipe` 매핑 풀어야 함) → 그래야 시드 레시피가 화면에 보임
- OAuth provider unlink + 만료된 refresh token row 정리 스케줄러 (계속 이월)

---

### 2026-05-08 — 만료된 refresh token 자동 정리 스케줄러

**무엇을 했나**: rotation 정책상 재발급 때마다 옛 토큰은 즉시 삭제되지만, 사용자가 14일간 재방문하지 않으면 만료된 row가 그대로 남는다. 매일 새벽 3시(서버 시간) 만료된 `refresh_tokens` row를 일괄 삭제하도록 스케줄러 추가.

#### 동작
- `RefreshTokenCleanupScheduler.purgeExpiredTokens()` — `@Scheduled(cron = "0 0 3 * * *")`
- 삭제된 행 수가 1 이상이면 `INFO` 로그로 기록 (모니터링/디버깅 용도)
- 0건이면 조용히 스킵 (로그 도배 방지)

#### 신규 / 수정 파일
- 신규: `user/RefreshTokenCleanupScheduler.java`
- 수정: `NaengbuhaeApplication.java` — `@EnableScheduling` 추가
- 수정: `user/RefreshTokenRepository.java` — `deleteAllExpiredBefore(LocalDateTime)` 메서드 추가 (`@Modifying` JPQL bulk delete)

#### 운영 메모
- cron 시간대는 서버 JVM 기본 시간대 기준. 프로덕션 배포 시 서버 TZ가 UTC면 한국 정오에 실행됨 — 실제 트래픽 적은 시간으로 옮기려면 `application-prod.properties`에서 `spring.task.scheduling.*` 또는 cron만 조정 가능.
- 별도 DB 마이그레이션 불필요 (스키마 변경 없음).

---

### 2026-05-08 (16) — GlobalExceptionHandler 통합 테스트 (@WebMvcTest)

**무엇을 했나**: `(4)`에서 보강한 전역 예외 핸들러의 각 분기를 실제 컨트롤러 호출 → 응답 형태까지 검증. 유닛 테스트와 달리 Spring MVC 한 슬라이스를 띄워서 진짜 라우팅 + 검증 + advice 체인이 동작하는지 본다.

#### 구성

```java
@WebMvcTest(IngredientController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)  // SecurityConfig/JwtFilter 우회
```

- `IngredientController`를 타깃으로 — 다양한 경로/파라미터/검증을 테스트하기에 좋음
- SecurityConfig가 필요로 하는 의존성(`JwtUtil`, `JwtAuthenticationFilter`, `CustomOAuth2UserService`, `OAuth2SuccessHandler`)은 빈 그래프 구성용 `@MockBean`
- `Principal`은 `() -> "alice"` 람다로 직접 주입

#### 테스트 (6개)

| 케이스 | 응답 | 검증 포인트 |
|---|---|---|
| `POST /api/ingredients` 빈 body | 400 | 다중 field 에러가 `;`로 합쳐짐 |
| 서비스에서 `IllegalArgumentException` 발생 | 400 | 서비스 메시지가 응답에 그대로 |
| `DELETE /api/ingredients/abc` (path 타입 불일치) | 400 | "Long" 안내 메시지 |
| malformed JSON body | 400 | "입력값" 안내 메시지 |
| 서비스에서 `RuntimeException` 발생 | 500 | **일반 메시지만**, 내부 디버그 메시지는 응답에 노출 X |
| 정상 흐름 (참고) | 200 | sanity check |

#### 의도

핵심 검증: **catch-all 분기에서 스택트레이스/내부 메시지가 응답에 새지 않는지** — 이건 보안상 중요한 지점이라 코드 리뷰만으로는 놓치기 쉬움.

#### 누적 테스트
- 유닛 63 + **통합 6** = **69개**, ~3.5초

```bash
./gradlew test
```

---

### 2026-05-08 (15) — UserService 유닛 테스트 (signup 검증 + 탈퇴 cascade + Kakao unlink)

**무엇을 했나**: signup의 비즈니스 검증 분기, deleteMyAccount의 cascade + Kakao unlink 분기 처리, updateMyProfile의 칼로리 재계산을 모두 커버.

#### `UserServiceTest` (11개)

`signup` (5):
- 아이디 중복 → "이미 존재하는 아이디입니다."
- 이메일 중복 → "이미 사용 중인 이메일입니다."
- 비번 한글 포함 → "비밀번호에 한글은 사용할 수 없습니다."
- 비번 패턴 위반 (특수문자 누락) → "8자 이상..."
- 정상 가입 → "회원가입 성공" + `passwordEncoder.encode` + `userRepository.save` 호출

`deleteMyAccount` (4):
- 사용자 없음 → 예외
- LOCAL 사용자 → cascade delete 4개(ingredient/recipe/shopping/refresh) + userRepo.delete, **Kakao unlink는 호출 안 됨**
- KAKAO 사용자 → cascade + **`kakaoUnlinkClient.unlink(providerId)` 호출됨**
- NAVER 사용자 → cascade만, Kakao unlink 호출 안 됨 (Naver/Google은 추후 작업)

`updateMyProfile` (2):
- 사용자 없음 → 예외
- 정상 update: `User.updateProfile` 호출 + `setRecommendedCalories` 호출(칼로리 재계산)

#### 누적 테스트
- AllergyMatcher 16 + Calorie 8 + Ip 7 + RefreshToken 10 + Recipe 7 + Ingredient 4 + **User 11** = **63개**, ~3초

```bash
./gradlew test
```

---

### 2026-05-08 (14) — RecipeService + IngredientService 유닛 테스트

**무엇을 했나**: 두 핵심 서비스의 비즈니스 로직 분기를 모두 커버하는 유닛 테스트 추가. 알레르기 기능과 매칭 로직의 회귀 방지가 핵심.

#### `RecipeServiceTest` (7개)

`recommendRecipes` (5):
- 사용자 없음 → IllegalArgumentException
- 필수 재료 모두 보유 → matchRate 100, 매칭률 desc 정렬
- 필수 재료 누락 → matchRate 0
- **만료된 식재료는 보유 인정 안 됨** (만료 우유 + 신선 계란인데 우유 필수면 matchRate=0)
- **알레르기 매칭 레시피는 결과에서 제외** (사용자가 "땅콩" 알레르기면 "땅콩잼토스트"는 결과에 안 들어감)

`findAllRecipes` (2):
- 사용자 없음 → 예외
- 알레르기 매칭된 키워드를 각 레시피의 `allergyWarnings`에 첨부 (필터링은 안 함, 표시만)

#### `IngredientServiceTest` (4개)

`saveIngredient` (2):
- 알레르기 매칭 식재료 등록 시 응답에 `allergyWarnings: ["땅콩"]` 채워짐
- 매칭 안 되는 식재료는 빈 배열

`findAllIngredients` (1):
- 다중 알레르기("땅콩, 우유") + 다중 식재료에서 각 항목별 매칭만 정확히 첨부

`findExpiring` (1):
- 만료된 것 + N일 이내 임박 모두 포함, 범위 밖/유통기한 null은 제외, 임박순 정렬

#### 누적 테스트
- AllergyMatcher 16 + Calorie 8 + Ip 7 + RefreshToken 10 + **Recipe 7** + **Ingredient 4** = **52개**, ~2초

```bash
./gradlew test
```

---

### 2026-05-08 (13) — RefreshTokenService 유닛 테스트 (재사용 탐지 분기)

**무엇을 했나**: `RefreshTokenService.refresh()`의 5개 분기와 `revoke()`/`issue()` 동작을 모두 커버하는 유닛 테스트 추가. `(11)`에서 도입한 재사용 탐지 로직의 회귀 방지가 핵심.

#### 신규 테스트 (10개)

`RefreshTokenServiceTest` — Mockito + `@InjectMocks`로 Spring context 없이 빠르게:

| 그룹 | 케이스 |
|---|---|
| `issue` | UUID 32자 형식 + DB 저장 검증 |
| `refresh` | 미존재 / 자연 만료 / 폐기-grace내 / 폐기-grace초과(재사용 탐지) / 정상 rotation |
| `revoke` | 활성 토큰 마킹 / 이미 폐기된 토큰 idempotent / 미존재 토큰 무시 |
| `revokeAllForUser` | 사용자의 모든 row 삭제 호출 |

#### 핵심 설계

- **`@Value` 필드 주입**: `ReflectionTestUtils.setField`로 `refreshTokenExpirationMs`(365일), `reuseGraceSeconds`(30초) 설정
- **User 엔티티 mock**: `mock(User.class)`로 생성, `lenient()`로 strict mode 충돌 방지 (getId()는 일부 분기에서만 사용)
- **revokedAt 시간 조작**: `ReflectionTestUtils.setField(token, "revokedAt", ...)` 으로 grace 내/외 상황 시뮬

#### 검증 포인트
- 정상 rotation 시 옛 토큰이 **`delete`되지 않고 `revoke()` 마킹**되는지 (재사용 탐지를 위해 row 보존)
- grace 5초 내 재제출은 `deleteByUser` 호출 안 됨, 5분 후 재제출은 호출됨

#### 누적 테스트
- AllergyMatcher 16 + Calorie 8 + Ip 7 + **RefreshToken 10** = **41개**, ~1.5초

```bash
./gradlew test
```

---

### 2026-05-08 (12) — 추가 유닛 테스트 (CalorieCalculator + ClientIpUtil)

**무엇을 했나**: 두 util 클래스에 단위 테스트 추가. AllergyMatcher 테스트(`(9)`)에 이어 회귀 방지 범위 확대.

#### 신규 테스트

`CalorieCalculatorTest` (8개) — `@Nested`로 토픽별 그룹화:
- **식단 목표** (4개): 체중 감량/근육량 증가 조정값 정확도, 건강 관리=체중 유지 동치, 미정의값 fallback
- **성별** (1개): 같은 조건에서 남 > 여 (BMR 공식 차이 반영)
- **활동량** (2개): 5단계 multiplier 오름차순, 미정의값은 거의 움직임 없음(1.2)으로 fallback
- **정수 반환** (1개): 합리적 범위(1000-5000kcal)

`ClientIpUtilTest` (7개) — Mockito로 `HttpServletRequest` mock:
- X-Forwarded-For 단일/다중 IP (콤마 split, 첫 번째 반환)
- 헤더 우선순위 (X-Forwarded-For → Proxy-Client-IP → WL-Proxy-Client-IP → HTTP_CLIENT_IP → HTTP_X_FORWARDED_FOR)
- 모든 헤더 없으면 `getRemoteAddr()` fallback
- "unknown" / 빈 문자열은 무시하고 다음 헤더 시도

#### 의도

- 절대값으로 검증하면 시간 흐름에 따라 깨지므로 (`LocalDate.now()` 사용) **상대적 차이**로 검증. 예: 체중 감량과 체중 유지의 차이는 항상 정확히 500kcal.
- 합쳐서 31개 테스트 (AllergyMatcher 16 + Calorie 8 + Ip 7), 실행 시간 ~1.2초

#### 실행
```bash
./gradlew test --tests "com.example.Naengbuhae.util.*"
```

---

### 2026-05-08 (11) — Refresh token 재사용 탐지

**무엇을 했나**: rotation 시 기존 토큰을 즉시 삭제하던 걸 `revokedAt` 마킹으로 바꾸고, 폐기된 토큰이 다시 들어오면 도난 시나리오로 간주해 해당 사용자의 모든 refresh token을 무효화. 1년 만료(`(8)`)로 늘어난 토큰의 보안 부담을 줄이는 목적.

#### 동작 흐름

`POST /user/token/refresh` 진입 시:

1. **존재하지 않는 토큰** → `유효하지 않은 refresh token입니다.`
2. **이미 폐기된 토큰** (`revokedAt != null`):
   - **grace period 내** (기본 30초): multi-tab race로 추정. invalid 처리만 하고 도난 처리 안 함.
   - **grace 초과**: **재사용 탐지**. 사용자의 모든 refresh token 즉시 삭제 → "세션이 무효화되었습니다" 응답
3. **자연 만료** (`expiresAt < now`) → 만료 응답
4. **정상**: 기존 토큰 `revoke()` 처리 + 새 access/refresh 발급

#### 왜 grace period?

`apiClient`의 `inflightRefresh`는 **한 탭 내**에서만 중복 호출을 막음. 여러 탭이 동시에 401을 받으면 같은 refresh token으로 동시에 `/user/token/refresh`를 호출 — 한 탭은 성공, 나머지 탭은 이미 폐기된 토큰을 다시 제출. 이걸 도난으로 처리하면 정상 사용자가 강제 로그아웃됨.

기본 30초 grace로 multi-tab race는 정상 처리, 진짜 도난(시간 두고 시도)만 탐지.

#### 신규 / 수정 파일
- 수정: `user/RefreshToken.java` — `revokedAt` 필드 + `isRevoked()` / `revoke()` 메서드 추가
- 수정: `user/RefreshTokenRepository.java` — cleanup 쿼리를 `expiresAt < now OR revokedAt < cutoff`로 확장 (메서드명도 `deleteExpiredOrOldRevoked`로 변경)
- 수정: `user/RefreshTokenService.java` — `refresh()`에 재사용 탐지 분기. 정상 rotation은 `delete` 대신 `revoke()`. `revoke(token)` 로그아웃도 마킹으로 변경
- 수정: `user/RefreshTokenCleanupScheduler.java` — 새 시그니처 사용, 폐기 보존 기간(`revoked-retention-hours`) 설정값 도입
- 수정: `application.properties` — `app.refresh-token.reuse-grace-seconds` (30), `app.refresh-token.revoked-retention-hours` (24) 추가

#### ⚠️ DB 마이그레이션

`spring.jpa.hibernate.ddl-auto=update`이 nullable 컬럼은 자동 추가하므로 별도 SQL은 필요 없습니다. 그래도 명시적으로 적어두면:

```sql
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;
```

#### 검증 시나리오 (수동)

1. 로그인 → DB에서 refresh_tokens row 1개 확인 (`revoked_at = NULL`)
2. access token 만료 후 API 호출 → 자동 refresh
3. DB 확인: 옛 row의 `revoked_at`이 채워졌고, 새 row가 추가됨
4. 옛 (폐기된) refresh token으로 30초 이내 다시 `/user/token/refresh` 호출 → "유효하지 않은 refresh token" (grace)
5. 옛 토큰으로 30초 후 다시 호출 → "세션이 무효화되었습니다" + DB의 해당 사용자 row 모두 삭제

#### 한계
- **분산 환경 미지원**: 단일 인스턴스 DB 기반이라 multi-instance에서도 동작은 하지만, race window가 인스턴스 간 시계 차이만큼 늘어남.
- grace period(30초)가 적당한지 운영하면서 조정 필요. 너무 짧으면 정상 다탭 사용자가 로그아웃됨, 너무 길면 도난 탐지 둔해짐.

---

### 2026-05-08 (10) — N+1 쿼리 최적화

**무엇을 했나**: 레시피 조회 메서드들에서 발생하던 N+1 쿼리 문제를 두 가지 방법으로 해결.

#### 문제 (Before)

| 메서드 | 발생 쿼리 수 (N개 레시피) | 원인 |
|---|---|---|
| `findByUser` (`/api/recipes`) | `1 + 3N` | user(N) + ingredients(N) + steps(N) 모두 lazy |
| `findAllWithUser` (admin `/admin/recipes`) | `1 + 2N` | user는 fetch, ingredients(N) + steps(N) lazy |
| `findAllWithUserAndIngredients` (`/api/recipes/recommendations`) | `1 + N` | user/ingredients fetch, steps(N) lazy |

#### 해결 방법

**1. Hibernate `default_batch_fetch_size=100`** (`application.properties`)

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=100
```

lazy collection이 로드될 때 Hibernate가 IN 절(`WHERE recipe_id IN (?,?,?,...)`)로 묶어서 한 번에 fetch. **모든 lazy 컬렉션에 자동 적용** — 코드 한 줄도 안 바꾸고 ingredients/steps의 N+1 제거.

**2. `RecipeRepository.findByUser`에 `@EntityGraph(user)` 추가**

`@OneToMany`/`@ElementCollection` 두 개를 동시에 JOIN FETCH하면 `MultipleBagFetchException`이 나기 때문에, 단일 `@ManyToOne user`만 EntityGraph로 즉시 fetch. ingredients/steps는 batch fetching으로 처리.

#### 결과 (After)

| 메서드 | 쿼리 수 | 비고 |
|---|---|---|
| `findByUser` | **3** | recipe+user 1개 + ingredients 배치 1개 + steps 배치 1개 |
| `findAllWithUser` | **3** | 기존 JOIN FETCH(user) + ingredients/steps 배치 |
| `findAllWithUserAndIngredients` | **2** | 기존 JOIN FETCH(user, ingredients) + steps 배치 |

#### 신규 / 수정 파일
- 수정: `application.properties` — batch_fetch_size 추가
- 수정: `repository/RecipeRepository.java` — `findByUser`에 `@EntityGraph(user)`

#### 검증
- 컴파일 + 기존 AllergyMatcher 테스트 통과
- 실제 N+1 효과는 `spring.jpa.show-sql=true` 환경에서 `/api/recipes` 호출 후 콘솔의 SELECT 개수로 확인 가능

#### 추가 개선 여지
- 데이터가 더 늘어나면 `recipe.steps`를 `String` (예: 줄바꿈으로 join된)으로 평탄화하거나 별도 lazy DTO 패턴 도입
- `findAll` 류에 페이지네이션 도입 (배치 사이즈와 별개로 페이로드 자체를 줄이는 효과)

---

### 2026-05-08 (9) — AllergyMatcher 유닛 테스트 (테스트 인프라 시작점)

**무엇을 했나**: 직전에 만든 `AllergyMatcher` 유틸에 단위 테스트 16개 추가. 그동안 비어있다시피 한 테스트 코드 공간(`contextLoads()` 한 줄뿐)에 첫 의미 있는 테스트.

#### 테스트 파일
- `src/test/java/com/example/Naengbuhae/util/AllergyMatcherTest.java`
- JUnit 5 + AssertJ (이미 `spring-boot-starter-test`에 포함)
- `@Nested` 클래스로 메서드별 그룹화 — `ParseAllergens`(6개), `FindMatches`(10개)
- Spring context 안 띄움 → 빠른 실행 (~0.1초)

#### 커버한 케이스 (요약)

`parseAllergens`:
- null/blank/공백 → 빈 Set
- 단일 키워드 lowercase 변환
- 콤마/세미콜론/슬래시/공백 모두 구분자
- 중복 제거, trim, 빈 토큰 제거

`findMatches`:
- 빈 알레르기/식재료 → 빈 결과
- 정확 일치, 부분 매칭(양방향)
- 매칭 없을 때
- 여러 알레르기 중 일부만 매칭, 같은 알레르기가 여러 식재료에 걸려도 한 번만
- 대소문자 무시
- null 식재료 항목 스킵 (방어)

#### 실행 방법
```bash
./gradlew test --tests AllergyMatcherTest
```

#### 의도
- 향후 다른 유틸/서비스 테스트 작성 시 **이 파일을 참고 패턴으로** 활용
- `AllergyMatcher`에 대한 회귀 방지 — substring 매칭 같은 미묘한 로직이 깨지지 않도록
- 추후 카테고리 사전 같은 기능을 추가하면 같은 테스트 클래스에 케이스 늘리면 됨

---

### 2026-05-08 (8) — Refresh token 만료 14일 → 365일 (로그인 유지 UX)

**무엇을 했나**: 프론트의 "로그인 상태 유지" 체크박스 도입에 맞춰 refresh token 기본 만료를 **14일 → 365일**로 변경.

#### 변경 사항
- `application.properties`: `app.jwt.refresh-token-expiration-ms` 기본값 `1209600000` → `31536000000`
- `RefreshTokenService`: `@Value` fallback 값도 동일하게 갱신

#### 동작
- 활성 사용자 (앱을 365일 안에 한 번이라도 사용): rotation으로 토큰이 매번 갱신되어 **사실상 영구 로그인**
- 365일간 미사용: 재로그인 강제 (사실상 자연 잊힘)
- 프론트에서 "로그인 상태 유지" 미체크 시: refresh token이 sessionStorage에 들어가 브라우저 종료 시 휘발 → 백엔드 만료 정책과 무관하게 즉시 로그아웃

#### 운영 메모
- DB의 `refresh_tokens.expires_at`은 발급 시점에 계산되므로, **이미 발급된 14일짜리 토큰은 그대로 14일 후 만료됨**. 이 변경 이후 새로 발급되는 토큰만 365일 적용
- `.env`에 `REFRESH_TOKEN_EXPIRATION_MS`로 override 가능 (기존 그대로)
- DB cleanup 스케줄러는 이미 만료된 row만 삭제하므로 영향 없음

---

### 2026-05-08 (7) — 알레르기 기능 활성화

**무엇을 했나**: 그동안 입력만 받고 사용처 0이었던 `User.allergies` 필드를 실제 사용. 사용자의 알레르기 텍스트를 파싱해 식재료/레시피 이름과 부분 매칭(substring), 결과를 응답에 첨부하거나 추천에서 제외.

#### 0단계 — 공통 매처 (`util/AllergyMatcher`)

```java
parseAllergens("땅콩, 갑각류 / 우유")  // → {"땅콩","갑각류","우유"}
findMatches(allergens, ingredientNames)  // → 매칭된 알레르기 키워드 Set
```
- 콤마/세미콜론/슬래시/공백 모두 구분자로 인식
- substring 매칭 (양방향) — `"땅콩"` 알레르기가 `"땅콩버터"` 식재료를 잡아냄
- 한계: 카테고리 사전 없음 → `"우유" → 치즈/요거트` 같은 추론은 못함. 사용자가 키워드를 잘 적는 걸 전제

#### 1단계 — 레시피 응답에 `allergyWarnings`

- `RecipeResponseDto`, `RecipeMatchResponseDto`에 `List<String> allergyWarnings` 필드 추가 (비어있으면 안전)
- `GET /api/recipes` (내 레시피), `GET /api/recipes/recommendations` 모두 응답에 포함
- 프론트는 이 배열이 비어있지 않으면 경고 배지/문구 표시 가능

#### 2단계 — 추천에서 알레르기 레시피 제외

- `recommendRecipes`에서 `allergyWarnings`가 비어있지 않은 레시피는 결과에서 필터링
- 안전 우선 원칙. 전체 레시피 목록(`GET /api/recipes`)에선 경고만 표시되고 그대로 보여줌
- 단점: 알레르기가 많아 모든 추천이 빠질 수 있음 → 그땐 사용자가 전체 레시피 페이지에서 직접 고르면 됨

#### 3단계 — 식재료 응답에 `allergyWarnings`

- `IngredientResponseDto`에 `List<String> allergyWarnings` 필드 추가
- `POST /api/ingredients` 응답이 **`Long` ID → `IngredientResponseDto`로 변경** (⚠️ breaking change)
- `GET /api/ingredients` 응답에도 동일 필드 포함
- 식재료 등록 직후 사용자에게 즉시 알레르기 매칭 알림 가능 (예: "땅콩버터" 등록 시 `["땅콩"]` 반환)

#### ⚠️ 프론트 영향
- `POST /api/ingredients`: 응답이 `123` 같은 숫자 → `{id: 123, name: "...", allergyWarnings: [...]}` 객체로 변경. 프론트에서 `data.id`로 ID 접근하도록 수정 필요.
- 그 외 응답들은 **추가 필드만 있는 비파괴적 변경** — 프론트가 무시해도 동작은 그대로

#### 신규 / 수정 파일
- 신규: `util/AllergyMatcher.java`
- 수정: `dto/RecipeResponseDto.java`, `dto/IngredientResponseDto.java` — `allergyWarnings` 필드 추가
- 수정: `service/RecipeService.java` — `findAllRecipes`, `recommendRecipes`에서 알레르기 적용 + 추천 필터링
- 수정: `service/IngredientService.java` — `saveIngredient`/`findAllIngredients`에서 알레르기 적용
- 수정: `controller/IngredientController.java` — `create` 응답 타입 `Long` → `IngredientResponseDto`

#### 추후 확장 가능
- 카테고리 사전 (`"우유" → 유제품 카테고리`) — 정확도↑
- `ExpiringIngredientResponseDto`에도 `allergyWarnings` 추가
- 가입/프로필 수정 시 알레르기 입력을 freetext가 아닌 칩 selector로 전환 (정규화 도움)

---

### 2026-05-08 (6) — 인증 엔드포인트 Rate Limiting

**무엇을 했나**: `/user/login`, `/user/signup`, `/user/token/refresh`에 IP별 호출 빈도 제한 추가. 로그인 무차별 시도(brute force) 차단 + DB 조회 자체를 막아 부하도 절감.

#### 구현
- 라이브러리: **Bucket4j** (Token Bucket 알고리즘) `8.10.1`
- `RateLimitFilter` (`OncePerRequestFilter`) — JWT 필터보다 앞에 위치. 인증/조회 일어나기 전에 차단.
- `(IP + path)` 조합을 키로 `ConcurrentHashMap<String, Bucket>`에 bucket 할당
- IP 추출은 `ClientIpUtil`(X-Forwarded-For 등 프록시 헤더 우선) 재활용

#### 제한 정책 (분당 IP당)

| 엔드포인트 | 분당 호출 |
|---|---|
| `/user/login` | 5회 |
| `/user/signup` | 5회 |
| `/user/token/refresh` | 10회 (정상은 30분에 1회 — 멀티탭/재시도 여유) |

초과 시 `429 Too Many Requests` + JSON `{success:false, message:"요청이 너무 많습니다..."}`.

#### 신규 / 수정 파일
- 신규: `config/RateLimitFilter.java`
- 수정: `config/SecurityConfig.java` — `addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)`
- 수정: `build.gradle` — `com.bucket4j:bucket4j-core:8.10.1` 추가

#### 한계 / 추후 작업
- **단일 인스턴스 메모리 기반** — 다중 인스턴스로 확장 시 bucket이 인스턴스끼리 공유되지 않으므로 IP당 N배 허용됨. Redis(`bucket4j-redis`)로 분산 저장 필요.
- 메모리에 bucket이 무한정 쌓일 수 있음(IP는 무한). 현재는 단일 인스턴스라 큰 문제 없지만, 트래픽 늘면 주기적 정리 스케줄러 추가 필요.
- 정상 사용자가 NAT 뒤에 여러 명 있는 환경(학교/카페 공용망)에선 IP가 같아 같이 차단될 수 있음. 5회/분이면 일반 사용엔 충분히 여유.

---

### 2026-05-08 (5) — DTO 입력값 검증 보강

**무엇을 했나**: 각 RequestDto의 검증 갭을 메우는 작은 수정들. 가장 큰 갭은 `LoginRequest`에 검증이 0개였던 점. 그 외 자유 텍스트의 길이 cap, 생년월일 미래 차단, 배열 크기 cap, 누락된 검증 메시지 등.

#### 주요 변경

| 파일 | 추가된 검증 |
|---|---|
| `LoginRequest` | `@NotBlank` on username/password (이전엔 검증 0개), 컨트롤러에 `@Valid` 추가 |
| `SignupRequest` | `birthDate @Past`, name/email/password/allergies 길이 cap |
| `ProfileUpdateRequest` | `birthDate @Past`, name/allergies 길이 cap |
| `IngredientRequestDto` | name(50)/unit(20) 길이 cap |
| `ShoppingItemRequestDto` | name(50)/unit(20) 길이 cap |
| `RecipeRequestDto` | name(100)/imageUrl(1000) 길이, steps 배열(50개)·각 단계 텍스트(1000자), ingredients 배열(100개) cap |
| `RecipeIngredientDto` | name(50)/unit(20) 길이 cap |
| `NutritionDto` | 누락된 `@PositiveOrZero` 메시지 모두 추가 |

#### 의도
- API 경계에서 **거대 페이로드 차단** — 자유 텍스트 길이 cap으로 1MB 알레르기 같은 abuse 방지
- **전역 예외 처리 강화와 결합** — 검증 실패 시 모든 field 에러를 한 번에 사용자에게 안내 (이전 라운드)
- DB 컬럼 길이와 정합성 — `User.allergies`는 `length=1000`, DTO도 1000으로 매칭

#### 신규 / 수정 파일
- 수정: `user/LoginRequest.java`, `user/SignupRequest.java`, `user/ProfileUpdateRequest.java`, `user/UserController.java` (login에 `@Valid` 추가)
- 수정: `dto/IngredientRequestDto.java`, `dto/ShoppingItemRequestDto.java`, `dto/RecipeRequestDto.java`, `dto/RecipeIngredientDto.java`, `dto/NutritionDto.java`

---

### 2026-05-08 (4) — 전역 예외 처리 보강

**무엇을 했나**: 기존 `GlobalExceptionHandler`가 4개 예외만 잡고 있었는데, 잡지 못한 예외들이 기본 500 응답으로 떨어지면서 스택트레이스가 노출될 위험이 있었음. 누락된 케이스를 메꾸고 다중 필드 에러도 한 번에 보여주도록 보강.

#### 새로 추가된 핸들러

| 예외 | 상태 코드 | 처리 |
|---|---|---|
| `Exception` (catch-all) | 500 | 사용자엔 일반 메시지, 서버 로그엔 스택트레이스 (`log.error`) |
| `AccessDeniedException` | 403 | `@PreAuthorize` 실패. JSON 응답으로 통일 (default는 HTML) |
| `ConstraintViolationException` | 400 | `@Validated`가 붙은 path/query 파라미터 검증 실패 |
| `MissingServletRequestParameterException` | 400 | 필수 `@RequestParam` 누락 시 어떤 파라미터가 빠졌는지 안내 |
| `MethodArgumentTypeMismatchException` | 400 | path variable / query param 타입 불일치 (`/api/recipes/abc`) |

#### 개선된 핸들러

- `MethodArgumentNotValidException` — 첫 번째 field 에러만 반환하던 걸 모든 field 에러를 `; `로 합쳐서 한 번에 노출. 다중 입력 폼에서 모든 잘못된 곳을 한 번에 수정 가능.

#### 응답 형식
- 모든 핸들러가 동일한 `ApiResponse(success: false, message: ...)` 사용 → 프론트는 핸들러 종류 신경 안 쓰고 통일된 처리 가능
- HTTP 상태 코드만 기준으로 분기: 400(잘못된 입력) / 403(권한 없음) / 409(중복) / 500(서버 에러)

#### 신규 / 수정 파일
- 수정: `exception/GlobalExceptionHandler.java`

---

### 2026-05-08 (3) — OAuth provider unlink (회원 탈퇴 시 카카오 연결 해제)

**무엇을 했나**: 회원 탈퇴 시 우리 DB에서 사용자 데이터를 지우는 것에 더해, 카카오로 가입한 사용자라면 카카오 측 연결도 해제(`unlink`) 호출. providerId만으로 호출 가능한 카카오 Admin Key 방식 사용 — provider access token을 따로 저장할 필요 없음.

#### 동작
- `UserService.deleteMyAccount()` 끝에 `provider == KAKAO`이면 `KakaoUnlinkClient.unlink(providerId)` 호출
- best-effort: 호출 실패해도 예외 던지지 않고 `WARN` 로그만 남김. 탈퇴 자체는 이미 진행됨.
- `RestTemplate` connect/read timeout 3초/5초 → 카카오 서버 hang 시 트랜잭션 잠기는 것 방지

#### 설정 추가
- `application.properties`: `app.oauth.kakao.admin-key=${KAKAO_ADMIN_KEY:}`
- `.env`에 `KAKAO_ADMIN_KEY` 추가 필요 (카카오 개발자 콘솔 > 앱 설정 > 앱 키 > **Admin 키**)
- 미설정 시 unlink 자동 스킵 (탈퇴는 정상 진행, 카카오 측 연결만 남음 — 사용자가 카카오 계정 설정에서 직접 해제해야 함)

#### 신규 / 수정 파일
- 신규: `user/KakaoUnlinkClient.java`
- 수정: `user/UserService.java` — `deleteMyAccount` 끝에 unlink 호출 + provider 정보 스냅샷
- 수정: `application.properties` — Admin Key property 추가

#### 구글/네이버는 추후 작업
- 두 provider의 unlink/revoke API는 **사용자 access token**이 필요한데, 현재 OAuth 플로우에서는 프로바이더 토큰을 저장하지 않고 우리 JWT만 발급하고 있음.
- 옵션: (1) `OAuth2AuthorizedClientService` JDBC 구현 도입, (2) User 엔티티에 provider access/refresh token 컬럼 추가
- 양쪽 다 보안 영향(토큰 저장 = 추가 secret at rest)이 있어 별도 결정 필요. 그 전까지는 구글/네이버 사용자 탈퇴 시 우리 DB만 정리되고 provider 측 연결은 사용자가 직접 해제해야 함.

#### 검증 방법
- 카카오로 로그인 → MyCustom에서 회원 탈퇴
- 서버 로그: `[KakaoUnlink] 성공 providerId=...` 확인
- 같은 카카오 계정으로 다시 로그인 시 카카오 동의 화면이 다시 뜨면 정상 (이전 동의가 끊긴 것)

---

### 2026-05-08 (2) — 운영 설정 다듬기 (prod profile + Actuator)

**무엇을 했나**: 로컬(dev)과 운영(prod) 설정을 분리해서 prod에서는 SQL 로그 차단, 에러 응답 단순화, 헬스체크 endpoint를 제공하도록 정리.

#### Profile 구조
- 베이스: `application.properties` (로컬 dev 기본값 — 지금까지 쓰던 그대로)
- 운영: `application-prod.properties` (override만 담음)
- 활성화: 환경변수 `SPRING_PROFILES_ACTIVE=prod`로 prod 프로파일 켜짐. 미설정 시 dev로 자동 fallback.

#### prod에서 바뀌는 것
- `spring.jpa.show-sql=false`, `format_sql=false` — SQL 로그 차단 (콘솔 부담 + 민감정보 노출 방지)
- `logging.level.root=INFO`, `org.springframework.security=WARN`, `org.hibernate.SQL=WARN`
- `server.error.include-stacktrace=never`, `include-message=never`, `include-binding-errors=never` — 에러 응답에 stack trace나 내부 메시지 노출 차단
- `management.endpoint.health.show-details=never` — `/actuator/health`가 상태만 반환 (DB/디스크 같은 내부 컴포넌트 정보 숨김)

#### Actuator
- `/actuator/health`만 노출 (`management.endpoints.web.exposure.include=health`)
- `SecurityConfig`에 `/actuator/health` permitAll 추가 → 로드밸런서/헬스 프로브가 인증 없이 호출 가능
- 다른 endpoint(`/actuator/info`, `/actuator/metrics` 등)는 명시적으로 노출하기 전까지 차단

#### 신규 / 수정 파일
- 신규: `application-prod.properties`
- 수정: `application.properties` — `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`, Actuator 노출 규칙 추가
- 수정: `build.gradle` — `spring-boot-starter-actuator` 의존성 추가
- 수정: `config/SecurityConfig.java` — `/actuator/health` permitAll

#### 검증 방법
- 로컬: 그대로 실행 — `dev` 프로파일이라 기존과 동일 (SQL 로그 그대로 보임)
- prod 실행: `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun`
- 헬스체크: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`

---

## 기본 세팅하기 

## Java(JDK 17 버전을 사용) - Amazon Corretto 17   
링크: https://aws.amazon.com/ko/corretto/  
화면에서 'Amazon Corretto 17 다운로드' 링크나 버튼을 찾아서 클릭  
Windows x64 줄에 있는 .msi 파일을 클릭해서 다운로드  
다운받은 설치 파일을 실행   
설치가 다 끝났다면, 시스템이 자바를 인식할 수 있도록 명령 프롬프을 연다  
```
java -version
```
화면에 openjdk version "17.0.x" (그리고 옆에 Corretto 어쩌고) 하는 문구가 예쁘게 뜨면 대성공

## 개발 툴(IDE): IntelliJ IDEA (인텔리제이)를 사용 
터미널로 설치 
```
winget install JetBrains.IntelliJIDEA.Community
```
약관 동의(Y/N)가 나오면 Y를 누르고 조금만 기다리면, 알아서 다운로드 된다

## spring boot 세팅 

<img width="1919" height="948" alt="spring boot" src="https://github.com/user-attachments/assets/c4b2d36b-580f-48ad-b06f-0402c1b27ae5" />

## 데이터베이스 구축하기 

<img width="1919" height="881" alt="image" src="https://github.com/user-attachments/assets/79e92f87-0e9f-4a57-91f9-bf559dcd8126" />

<img width="1919" height="879" alt="image" src="https://github.com/user-attachments/assets/6c962256-c913-463c-bfe2-a1fa6cd1a241" />

<img width="1919" height="883" alt="image" src="https://github.com/user-attachments/assets/c6695bae-0ffa-401c-b291-d2190ed1d849" />

<img width="1919" height="876" alt="image" src="https://github.com/user-attachments/assets/72a180fb-2e02-4f07-be1f-f86526d50249" />



## 데이터 베이스랑 연결시키기 

🛠️ 1단계: 스프링 부트에게 PostgreSQL 번역기 달아주기
스프링 부트는 처음에 우리가 설정했던 H2 데이터베이스만 알고 있어서, PostgreSQL이랑 대화하려면 전용 번역기(드라이버)를 하나 달아줘야 해.

1. 인텔리제이 왼쪽 프로젝트 파일 목록에서 build.gradle 이라는 코끼리 모양 파일을 더블클릭해서 열어줘.

2. 코드 아래쪽으로 쭉 내리다 보면 dependencies { ... } 라고 적힌 블록이 보일 거야.

3. 그 괄호 { } 안에 아래 코드 한 줄을 복사해서 맨 밑에 추가해 줘.

```
runtimeOnly 'org.postgresql:postgresql'
```
4. [제일 중요 ⭐️] 코드를 붙여넣으면 인텔리제이 화면 오른쪽 위 구석에 작은 코끼리 아이콘(Load Gradle Changes) 🐘이 둥둥 뜰 거야. 그걸 무조건! 꼭! 눌러줘야 번역기가 다운로드 돼. (화면 아래쪽 상태 표시줄에 로딩 바가 다 지나갈 때까지 잠깐 기다려줘!)

<img width="959" height="503" alt="image" src="https://github.com/user-attachments/assets/6df1d047-a1bf-487b-8351-beac0cb7253f" />

🔗 2단계: 마법의 DB 주소 입력하기
이제 진짜 주소를 알려줄 차례야.

인텔리제이 왼쪽 파일 목록에서 src ➔ main ➔ resources 폴더를 열면, 그 안에 application.properties (또는 application.yml) 파일이 있을 거야. 더블클릭해서 열어줘.

파일 안에 아래 코드를 통째로 복사해서 붙여넣어 줘.

```
# 데이터베이스 연결 주소 (여기에 아까 만든 주소를 넣을 거야!) 이 주소 절대 공개 금지(털려서...) ai한테도 당장 금지 나중에 .env파일을 분리하겠습니
spring.datasource.url=여기를_지우고_아까_완성한_긴_주소를_통째로_붙여넣어주세요
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA (스프링 부트 <-> DB 번역기) 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 재생 버튼을 눌러서 재생하기 
<img width="1915" height="53" alt="image" src="https://github.com/user-attachments/assets/9ae59589-778e-4278-98b6-46890b617c89" />

나 같은 경우 이렇게 저장을 했는데 불이 안 들어 왔다   

🟢 잠든 재생 버튼 깨우고 서버 켜는 법
1. 인텔리제이 왼쪽 폴더 목록에서 src ➔ main ➔ java ➔ com.example.어쩌구 폴더를 차례대로 열어줘.
2. 그 안에 보면 NaengbuhaeApplication (또는 이름이 비슷한 ~Application.java) 이라는 자바 파일이 딱 하나 있을 거야. 그걸 더블클릭해서 열어!
3. 파일이 열리면 코드 창 왼쪽 줄 번호 옆을 잘 봐. public static void main(String[] args) 라고 적힌 줄 바로 옆에 **초록색 재생 버튼(▶️)**이 귀엽게 붙어있을 거야.
4. 그 초록색 버튼을 클릭하고, 'Run NaengbuhaeApplication.main()' 을 선택해 줘!

근데 
```
Caused by: org.postgresql.util.PSQLException: The connection attempt failed.
```
이런 에러가   

🕵️ 원인: Supabase의 최신 정책 (IPv6) vs 우리 집 인터넷 (IPv4)
최근에 Supabase가 속도를 높이려고 'Direct connection(직접 연결)' 방식을 최신 인터넷 주소망(IPv6)으로 강제 업데이트했어. 그런데 우리나라의 많은 가정용 인터넷이나 와이파이는 아직 구형 주소망(IPv4)을 쓰는 경우가 많아서, 서로 대화가 안 통하고 튕겨버리는 거야.

💡 해결책: 'Session pooler (구형 인터넷용 터널)'로 바꿔주기!
아까 네가 나한테 캡처해서 보여줬던 화면(Connection Method 고르는 창) 혹시 기억나? 그 화면으로 딱 한 번만 다시 돌아가 보자!

1. Supabase 대시보드에서 아까 접속했던 [Connect] 창을 다시 열어줘.
2. Connection Method에서 Direct connection 대신, 맨 아래에 있는 Session pooler를 선택해!
(아까 네 캡처 화면에도 자세히 보면 "IPv4 네트워크 환경에서 연결할 때 추천함"이라고 적혀있었어!)
3. Type은 똑같이 JDBC로 둔 상태에서, 새롭게 짠! 하고 나타난 새로운 긴 주소를 복사해 줘. (이번엔 주소 끝부분 포트 번호가 5432가 아니라 6543으로 바뀌어 있을 거야!)
4. 복사한 주소에 아까처럼 진짜 비밀번호를 다시 끼워 넣어줘. (대괄호 [] 지우는 거 잊지 말고!)

🛠️ 마무리 인텔리제이 수정
이제 인텔리제이의 application.properties로 돌아와서, 방금 만든 새로운 Session pooler 주소로 싹 갈아끼워줘. 
```
spring.application.name=Naengbuhae

# 💡 포트번호가 6543으로 끝나는 Session pooler 주소로 변경!
spring.datasource.url=여기를_지우고_아까_완성한_긴_주소를_통째로_붙여넣어주세요
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 해결 
```
spring.application.name=Naengbuhae

spring.datasource.url=jdbc:postgresql://db.lulvkjjxtmnvvqvnatbp.supabase.co:6543/postgres

spring.datasource.username=postgres
# 데이터베이스 연결 주소 이 주소 절대 공개 금지(털려서...) ai한테도 당장 금지 나중에 .env파일을 분리하겠습니다
spring.datasource.password=여기에_비번을_넣어요

spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

이걸로 하고 휴대폰 핫스팟을 켜서 되니 연결이 됨


## .env 파일로 민갑한 정보 숨기기 

🛡️ 1단계: 스프링 부트한테 .env 읽는 법 가르치기
원래 Node.js 같은 건 .env를 바로 읽지만, 스프링 부트는 돋보기(라이브러리)를 하나 달아줘야 해.

1. 인텔리제이 왼쪽 파일 목록에서 🐘 build.gradle 파일을 열어줘.
2. dependencies { ... } 라고 적힌 블록 안에 아래 코드를 한 줄 추가해!
```
implementation 'me.paulschwarz:spring-dotenv:4.0.0'
```
3. 코드를 넣으면 화면 오른쪽 위에 작게 코끼리 아이콘(🐘)이랑 새로고침 버튼이 뜰 거야. 그걸 꼭! 눌러서 라이브러리를 설치해 줘. (밑에 게이지 다 찰 때까지 대기!)


📝 2단계: 최상위 폴더에 .env 파일 만들기
이제 진짜 비밀번호를 담을 금고를 만들 차례야.

1. 인텔리제이 왼쪽 파일 목록에서 프로젝트의 맨 꼭대기(루트) 폴더를 우클릭해. (아마 Naengbuhae라고 적힌 제일 위쪽 폴더일 거야. src 폴더 안이 아니야!)
2. [New] ➔ [File] 을 누르고, 파일 이름을 정확히 .env 라고 적고 엔터를 쳐.
3. 만들어진 .env 파일 안에 아래처럼 네 진짜 정보를 적어줘! (여긴 띄어쓰기나 따옴표 없이 적는 게 좋아)

🙈 3단계: 깃허브에서 .env 완벽하게 숨기기 (가장 중요!!!)
금고를 만들었으니, 깃허브라는 공개 광장에 이 금고가 올라가지 않도록 투명 망토를 씌워야 해!

1. 프로젝트 맨 꼭대기 폴더에 보면 .gitignore 라는 파일이 이미 있을 거야. (이게 투명 망토 파일이야!) 열어줘.
2. 파일 맨 아래 빈 공간에 딱 이렇게 한 줄을 추가해 줘.

```
# 환경변수 파일 숨기기
.env
```
이러면 깃허브 데스크탑 같은 곳에서 .env 파일이 아예 안 보이게 돼서, 절대 실수로 올라갈 일이 없어!  

🔄 4단계: application.properties 수정하기
이제 원래 파일로 돌아가서, "내 진짜 비밀번호는 .env 금고 안에 있으니까 거기서 꺼내 써!" 라고 연결해 주면 끝이야.

application.properties 파일을 열고 아까 적었던 부분을 이렇게 수정해 줘:

```
spring.application.name=Naengbuhae

# 주소는 안 가려도 됨!
spring.datasource.url=jdbc:postgresql://db.lulvkjjxtmnvvqvnatbp.supabase.co:6543/postgres

# .env 금고에서 가져오기! (달러 기호랑 중괄호 필수)
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 해결 

<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/ab1d3c4e-64e3-46ce-a471-e9894e1fe7ba" />

<img width="1198" height="990" alt="image" src="https://github.com/user-attachments/assets/933543ce-e8a6-4c0c-be90-da17866b4bf6" />
환경 변수에 직접 경로를 입력하니 해결이 되었다 

## 식제료 도메인 기초 세팅 

<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/b6394212-4c07-4a7b-a4d1-910358cce01e" />

Ingredient.java (domain 폴더) : 식재료 설계도
```
package com.example.Naengbuhae.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

// @Entity: "스프링아, 이 클래스 모양대로 Supabase DB에 '식재료' 테이블을 만들어줘!" 라는 뜻
@Entity
@Getter @Setter // 롬복(Lombok) 기능: 숨겨진 데이터(필드)를 꺼내고(Get) 바꿀(Set) 수 있게 해줌
@NoArgsConstructor // 롬복 기능: 텅 빈 기본 설계도(기본 생성자)를 알아서 만들어줌
public class Ingredient {

    // @Id: "이게 식재료들을 구분하는 고유 번호(주민등록번호)야!" 라는 뜻 (Primary Key)
    @Id
    // @GeneratedValue: "고유 번호는 내가 안 넣을 테니까, DB 네가 1, 2, 3... 알아서 1씩 올려가며 넣어줘!"
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: "이 데이터는 DB의 기둥(컬럼)이 될 건데, 이름은 무조건 있어야 해! (비어있으면 안 됨)"
    @Column(nullable = false)
    private String name; // 식재료 이름 (예: 계란)

    private Integer quantity; // 수량 (예: 10)

    private LocalDate expirationDate; // 유통기한 (예: 2026-04-15)

    // 식재료를 처음 만들 때 이름, 수량, 유통기한을 한 번에 쏙 넣기 위해 만든 틀(생성자)
    public Ingredient(String name, Integer quantity, LocalDate expirationDate) {
        this.name = name;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }
}
```

IngredientRepository.java (repository 폴더) : DB 창고지기
```
package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// @Repository: "스프링아, 얘는 DB 창고지기(Repository)니까 네가 관리해 줘!"
@Repository
// JpaRepository<Ingredient, Long>: 마법의 지팡이!
// "이 창고지기는 'Ingredient(식재료)' 데이터를 다룰 거고, 고유 번호는 'Long(숫자)' 타입이야."
// 이걸 상속(extends)받는 순간, 저장(save), 찾기(findById), 전체조회(findAll) 같은 SQL 코드를 안 짜도 다 쓸 수 있음!
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
```

IngredientService.java (service 폴더) : 프로젝트의 두뇌
```
package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @Service: "얘가 우리 프로그램의 비즈니스 로직(머리 쓰는 일)을 담당하는 애야!"
@Service
// @Transactional(readOnly = true): "여기 있는 기능들은 기본적으로 DB를 '읽기'만 할 거야. (조회 속도가 빨라짐!)"
@Transactional(readOnly = true)
// @RequiredArgsConstructor: 롬복 기능. 창고지기(Repository)를 자동으로 섭외해서 연결해 줌.
@RequiredArgsConstructor
public class IngredientService {

    // 두뇌(Service)가 일을 하려면 창고지기(Repository)가 무조건 필요함!
    private final IngredientRepository ingredientRepository;

    // --- 1. 식재료 저장 기능 ---
    // @Transactional: "이 기능은 DB에 데이터를 쓰는 거니까, 혹시 에러 나면 저장 취소(롤백)하고 완벽하게 처리해 줘!"
    @Transactional
    public Long saveIngredient(Ingredient ingredient) {
        ingredientRepository.save(ingredient); // 창고지기한테 "이 식재료 저장해!" 라고 시킴
        return ingredient.getId(); // 저장이 잘 끝났으면, DB가 부여한 고유 번호를 돌려줌
    }

    // --- 2. 식재료 전체 조회 기능 ---
    public List<Ingredient> findAllIngredients() {
        return ingredientRepository.findAll(); // 창고지기한테 "창고에 있는 식재료 싹 다 가져와!" 라고 시킴
    }
}
```

IngredientController.java (controller 폴더) : 레스토랑 안내 데스크
```
package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController: "얘는 외부(브라우저, Postman, AI 팀원)의 요청을 받는 API 안내 데스크야!"
@RestController
// @RequestMapping: "이 안내 데스크의 주소는 'http://localhost:8080/api/ingredients'야!"
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor // 두뇌(Service)를 자동으로 섭외해서 연결해 줌.
public class IngredientController {

    // 안내 데스크는 들어온 요청을 처리하기 위해 두뇌(Service)에게 일을 넘겨야 함!
    private final IngredientService ingredientService;

    // --- API 1: 식재료 새로 등록하기 (POST 요청) ---
    // @PostMapping: 누군가 이 주소로 POST(저장) 요청을 보내면 이 메서드가 실행됨
    @PostMapping
    // @RequestBody: "요청으로 날아온 JSON 데이터(계란 10개 등)를 Ingredient 객체로 찰떡같이 변환해서 받아줘!"
    public Long create(@RequestBody Ingredient ingredient) {
        // 두뇌(Service)에게 저장을 부탁하고, 성공하면 받은 고유 번호를 돌려줌
        return ingredientService.saveIngredient(ingredient);
    }

    // --- API 2: 냉장고 속 식재료 다 보기 (GET 요청) ---
    // @GetMapping: 누군가 이 주소로 GET(조회) 요청을 보내면 이 메서드가 실행됨
    @GetMapping
    public List<Ingredient> list() {
        // 두뇌(Service)에게 싹 다 찾아오라고 시킨 결과를 리스트 형태(JSON)로 뱉어줌
        return ingredientService.findAllIngredients();
    }
}
```



## 포트 번호 막히는 거 해결하기 
1️⃣ Supabase PostgREST API 사용하기 (비추천 🙅‍♂️)
원리: Supabase는 DB(5432 포트)를 직접 안 찔러도, 웹사이트 접속하는 것처럼 443 포트(HTTPS)로 데이터를 넣고 뺄 수 있는 'REST API' 기능을 기본으로 제공해. 학교 와이파이도 443 포트는 웹서핑을 해야 하니까 절대 못 막거든! 그래서 이 방법을 쓰면 와이파이에서도 뻥뻥 뚫려.

우리가 쓰면 안 되는 이유: 이걸 쓰려면 우리가 어제 피땀 흘려 만든 Spring Data JPA (IngredientRepository, @Entity 등)를 전부 다 버려야 해! 😭
JPA는 무조건 5432 포트로 DB랑 '직접 연결(JDBC)'을 해야만 작동하는 마법이거든. API 방식으로 바꾸면 코드를 처음부터 끝까지 다 갈아엎어야 해서 지금 상황에선 절대 비추천이야!

2️⃣ Cloudflare WARP 쓰기 (초강력 추천 🌟🌟🌟🌟🌟)
원리: 클라우드플레어(Cloudflare) 워프는 아주 쉽고 빠르고 **무료인 VPN(비밀 터널)**이야.

왜 해결될까?: 이걸 켜면 원희 컴퓨터에서 나가는 5432 포트 요청을 'Cloudflare'라는 거대한 비밀 보따리에 꽁꽁 싸매서 학교 와이파이 공유기를 통과해. 공유기는 "어? 그냥 클라우드플레어 웹사이트 가는 트래픽이네? 통과!" 하고 속아 넘어가는 거지!

개이득 포인트: 제일 중요한 건, 우리가 어제 짠 스프링 부트 코드를 단 1글자도 수정할 필요가 없다는 거야!! 게다가 최근에 Supabase가 무료 버전에서 IPv4 지원을 중단해서 연결이 까다로워졌는데, WARP를 쓰면 이 문제까지 한 방에 해결돼.  

## 테스트 
파워셀은 잘 안 되어서 cmd로 함
<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/988480ea-bbd1-47d4-b69f-2f787ffb6411" />

```
curl.exe -X POST http://localhost:8080/api/ingredients -H "Content-Type: application/json" -d "{\"name\": \"계란\", \"quantity\": 10, \"expirationDate\": \"2026-04-15\"}"
```

성공적으로 들어감

<img width="1919" height="998" alt="image" src="https://github.com/user-attachments/assets/afc74bb5-c4e3-418e-93fc-f5a7b3de9e75" />

## DTO(Data Transfer Object)를 사용해서 보안 올리기 
그러면 왜 써야 할까?  
DB에 있는 식재료 원본(Ingredient 엔티티)은 너무 소중해서 밖으로 함부로 내보내면 안 된다. 그래서 외부랑 데이터를 주고받을 때는 무조건 이 택배 상자(DTO)에 필요한 것만 딱 담아서 주고받는 거! (보안 + 깔끔함 상승!)  

IngredientRequestDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Ingredient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
public class IngredientRequestDto {

    // 외부에서 받을 데이터들만 딱 정의해 둬 (id는 DB가 알아서 넣을 거니까 안 받아도 됨!)
    private String name;
    private Integer quantity;
    private LocalDate expirationDate;

    // 편의 기능: "이 택배 상자(DTO)에 든 내용물을 실제 DB용 식재료(Entity)로 변환해 줘!"
    public Ingredient toEntity() {
        return new Ingredient(name, quantity, expirationDate);
    }
}
```
IngredientResponseDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Ingredient;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class IngredientResponseDto {
    private Long id;
    private String name;
    private Integer quantity;
    private LocalDate expirationDate;

    // 생성자: "DB에서 꺼낸 진짜 식재료(Entity)를 주면, 내가 택배 상자(DTO)에 예쁘게 옮겨 담을게!"
    public IngredientResponseDto(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.quantity = ingredient.getQuantity();
        this.expirationDate = ingredient.getExpirationDate();
    }
}
```
IngredientService.java(변경)
```
package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    // 1. 저장할 때: 원본 대신 '받는 택배 상자(RequestDto)'를 받음
    @Transactional
    public Long saveIngredient(IngredientRequestDto requestDto) {
        // 상자 내용물을 원본(Entity)으로 뜯어서 변환한 다음, DB 창고에 저장!
        return ingredientRepository.save(requestDto.toEntity()).getId();
    }

    // 2. 조회할 때: 원본 대신 '보내는 택배 상자(ResponseDto)' 리스트를 뱉음
    public List<IngredientResponseDto> findAllIngredients() {
        // DB 창고에서 원본들을 싹 꺼내온 다음, 하나하나 예쁜 택배 상자(DTO)에 옮겨 담아서(map) 반환!
        return ingredientRepository.findAll().stream()
                .map(IngredientResponseDto::new) // Ingredient 원본을 ResponseDto로 포장하는 마법의 코드
                .collect(Collectors.toList());
    }
}
```
IngredientController.java(변)
```
package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    // POST: 저장 요청이 오면 '받는 택배 상자(RequestDto)'로 안전하게 받기
    @PostMapping
    public Long create(@RequestBody IngredientRequestDto requestDto) {
        return ingredientService.saveIngredient(requestDto);
    }

    // GET: 전체 조회 요청이 오면 원본 말고 '보내는 택배 상자(ResponseDto)' 리스트로 안전하게 내보내기
    @GetMapping
    public List<IngredientResponseDto> list() {
        return ingredientService.findAllIngredients();
    }
}
```

<img width="1919" height="1012" alt="image" src="https://github.com/user-attachments/assets/a50bee7f-79cb-40b4-a153-65166135613e" />

## 결과 

<img width="1919" height="1013" alt="image" src="https://github.com/user-attachments/assets/a5dbbb5a-0b6b-4ac8-8d8a-43e4fffaf055" />

<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/33dfc39e-44e5-4062-b7fb-ec75c05c4ca8" />

성공 

## 삭제하기 만들기 
IngredientService.java(수정)
```
// --- 3. 식재료 삭제 기능 ---
    @Transactional
    public void deleteIngredient(Long id) {
        // 창고지기한테 "이 번호표(id) 가진 식재료 찾아서 버려!" 라고 시킴
        ingredientRepository.deleteById(id);
    }
```

IngredientController.java(수정)
```
// --- API 3: 식재료 삭제하기 (DELETE 요청) ---
    // @DeleteMapping: 누군가 주소 뒤에 번호(id)를 달고 DELETE 요청을 보내면 실행됨
    // 예: /api/ingredients/1 (1번 지워줘!)
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return id + "번 식재료가 냉장고에서 삭제되었습니다! 🗑️";
    }
```


<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/ee6a2ac1-8616-4992-9a36-718f2638072d" />

```
curl.exe -X DELETE http://localhost:8080/api/ingredients/1
```

<img width="1919" height="1003" alt="image" src="https://github.com/user-attachments/assets/7007d057-7d33-4c64-b9a2-0bc06b8d8926" />

## 식재료 수정(Update) 기능 추가 

IngredientService.java
```
// --- 4. 식재료 수정 기능 (Update) ---
    // @Transactional이 여기서 진짜 중요한 마법을 부림!
    @Transactional
    public Long updateIngredient(Long id, IngredientRequestDto requestDto) {
        // 1. 창고에서 수정할 식재료를 번호(id)로 찾아온다. (없으면 에러 뱉음!)
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 식재료가 없습니다. id=" + id));

        // 2. 찾아온 원본 식재료의 정보를 새 택배 상자(DTO)에 담긴 정보로 바꿔치기!
        ingredient.setName(requestDto.getName());
        ingredient.setQuantity(requestDto.getQuantity());
        ingredient.setExpirationDate(requestDto.getExpirationDate());

        // 3. 엥? 저장(save)을 안 하네?! 
        // 👉 맞음! 스프링 JPA의 '변경 감지' 마법 덕분에 값만 바꿔도 알아서 DB에 덮어씌워짐!
        return ingredient.getId();
    }
```

IngredientController.java
```
// --- API 4: 식재료 수정하기 (PUT 요청) ---
    // @PutMapping: 누군가 주소 뒤에 번호(id)를 달고 PUT(수정) 요청을 보내면 실행됨
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @RequestBody IngredientRequestDto requestDto) {
        // 두뇌(Service)에게 "id번 식재료를 이 새 정보(requestDto)로 바꿔줘!" 라고 시킴
        return ingredientService.updateIngredient(id, requestDto);
    }
```

## 계란 10개 -> 8개로 줄여보기 테스트!

<img width="1919" height="1004" alt="image" src="https://github.com/user-attachments/assets/150c3bb3-9af1-4368-880c-a1bb591c0ab1" />

성공 

```
curl.exe -X PUT http://localhost:8080/api/ingredients/2 -H "Content-Type: application/json" -d "{\"name\": \"계란\", \"quantity\": 8, \"expirationDate\": \"2026-04-15\"}"
```

<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/385d271d-b2dd-4292-9153-41f1fcb94d17" />

## 스웨거(Swagger) 

build.gradle  
dependencies { ... } 에 추가
```
// Swagger (springdoc-openapi)
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0'
```

<img width="400" height="201" alt="image" src="https://github.com/user-attachments/assets/79858825-803b-4fad-a991-4a8236d92141" />
코끼리 누르기   


## 레시피(Recipe) 도메인 1단계: 설계도 & 창고지기 만들기

Recipe.java 
```
package com.example.Naengbuhae.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 요리 이름 (예: 계란말이)

    @Column(columnDefinition = "TEXT") 
    private String instructions; // 만드는 법 (글자가 길어질 수 있으니 TEXT 타입으로!)

    private Integer cookingTime; // 조리 시간(분 단위, 예: 15)

    // 레시피 생성자
    public Recipe(String title, String instructions, Integer cookingTime) {
        this.title = title;
        this.instructions = instructions;
        this.cookingTime = cookingTime;
    }
}
```
RecipeRepository.java
```
package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}
```

## 레시피 전용 택배 상자(DTO) 2개랑, 두뇌(Service), 안내 데스크(Controller) 코드

RecipeRequestDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Recipe;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RecipeRequestDto {

    private String title;
    private String instructions;
    private Integer cookingTime;

    // DTO를 DB용 엔티티로 찰떡 변환!
    public Recipe toEntity() {
        return new Recipe(title, instructions, cookingTime);
    }
}
```

RecipeResponseDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Recipe;
import lombok.Getter;

@Getter
public class RecipeResponseDto {

    private Long id;
    private String title;
    private String instructions;
    private Integer cookingTime;

    // DB에서 꺼낸 엔티티를 이 DTO 상자에 예쁘게 포장!
    public RecipeResponseDto(Recipe recipe) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        this.instructions = recipe.getInstructions();
        this.cookingTime = recipe.getCookingTime();
    }
}
```

RecipeService.java
```
package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;

    // 1. 레시피 저장 (Create)
    @Transactional
    public Long saveRecipe(RecipeRequestDto requestDto) {
        return recipeRepository.save(requestDto.toEntity()).getId();
    }

    // 2. 레시피 전체 조회 (Read)
    public List<RecipeResponseDto> findAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(RecipeResponseDto::new)
                .collect(Collectors.toList());
    }
    
    // (일단 가장 기본이 되는 등록/조회만 뚫어둘게! 수정/삭제는 나중에 필요하면 추가!)
}
```

RecipeController.java
```
package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// 주의: 주소가 이번엔 /api/recipes 야!
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    // POST: 레시피 등록 API
    @PostMapping
    public Long create(@RequestBody RecipeRequestDto requestDto) {
        return recipeService.saveRecipe(requestDto);
    }

    // GET: 레시피 전체 조회 API
    @GetMapping
    public List<RecipeResponseDto> list() {
        return recipeService.findAllRecipes();
    }
}
```

## 테스트 

이제 터미널에서 까만 화면 보면서 curl 칠 필요 없어! 아까 우리가 달아둔 스웨거 메뉴판으로 가서 바로 테스트해 보자.

1. 크롬에서 http://localhost:8080/swagger-ui/index.html 새로고침!
2. 화면에 ingredient-controller 밑에 새로 생긴 recipe-controller 메뉴가 짠! 하고 나타난 걸 확인해.
3. 초록색 POST /api/recipes 누르고 [Try it out] 클릭!
4. 데이터 칸(Request body)에 요리 이름(title), 만드는 법(instructions), 조리 시간(cookingTime) 적당히 입력하고 [Execute] 파란 버튼 클릭!
5. 밑에 응답 결과(Response body)에 숫자 **1**이 딱 떨어지면 완벽하게 성공한 거야.

<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/d9b0b942-b6ab-499c-9e92-0b324ce5f8a8" />

## 지금까지 한 거 정리 
```
📦 Naengbuhae (스마트 냉장고 관리 백엔드)
 ┣ 📂 src/main/java/com/example/Naengbuhae
 │ ┣ 📂 controller       # 클라이언트의 요청을 받고 응답하는 안내 데스크
 │ │ ┣ 📜 IngredientController.java
 │ │ ┗ 📜 RecipeController.java
 │ │
 │ ┣ 📂 domain           # DB 테이블과 직접 연결되는 설계도 (Entity)
 │ │ ┣ 📜 Ingredient.java
 │ │ ┗ 📜 Recipe.java
 │ │
 │ ┣ 📂 dto              # 계층 간 데이터를 안전하게 주고받는 택배 상자
 │ │ ┣ 📜 IngredientRequestDto.java
 │ │ ┣ 📜 IngredientResponseDto.java
 │ │ ┣ 📜 RecipeRequestDto.java
 │ │ ┗ 📜 RecipeResponseDto.java
 │ │
 │ ┣ 📂 repository       # DB 창고에 접근해서 데이터를 넣고 빼는 창고지기
 │ │ ┣ 📜 IngredientRepository.java
 │ │ ┗ 📜 RecipeRepository.java
 │ │
 │ ┗ 📂 service          # 핵심 비즈니스 로직을 처리하는 두뇌
 │   ┣ 📜 IngredientService.java
 │   ┗ 📜 RecipeService.java
 │
 ┗ 📜 build.gradle       # 외부 라이브러리(Swagger 등) 의존성 관리
```
본 프로젝트는 엔티티(Entity)의 외부 노출을 막고 보안과 유연성을 높이기 위해, Controller와 Service 계층 간의 데이터 통신에 DTO(Data Transfer Object) 패턴을 적극적으로 도입하여 설계했습니다. 

DTO의 특징
- 데이터 전달 전용: 비즈니스 로직을 담지 않고, 오직 데이터만 담습니다.
- 보안성 강화: 엔티티를 그대로 노출하면 민감한 정보까지 외부에 드러날 수 있는데, DTO는 필요한 필드만 선택적으로 담아 전달합니다.
- 유연성 확보: 엔티티 구조가 바뀌더라도 DTO를 통해 외부 API나 클라이언트와의 계약을 안정적으로 유지할 수 있습니다.
- 변환 용이: 엔티티 ↔ DTO 간 변환을 통해 원하는 형태로 데이터를 가공할 수 있습니다.


## 코드 통합하기

com vs org, 뭐가 더 좋을까?
무조건 com으로 통일하는 걸 추천해! 1. 대세는 com: 졸업 프로젝트로 끝나는 게 아니라 나중에 앱이나 웹 서비스로 출시한다고 생각했을 때, 대부분의 스타트업이나 상용 서비스는 com을 표준으로 써.

JwtUtil.java,build.gradle,UserController.java,SecurityConfig.java등 바꾸고 .env파일 추

🛠️ 스웨거(메뉴판) '프리패스' 등록하기
config 폴더에 있는 SecurityConfig.java 파일을 열어서, .authorizeHttpRequests 부분을 아래처럼 살짝만 바꿔줘! 스웨거 관련 주소들을 프리패스(permitAll) 명단에 추가하는 거야.

[수정 전]
```
.authorizeHttpRequests(auth -> auth
                .requestMatchers("/user/signup", "/user/login").permitAll()
                .anyRequest().authenticated()
        )
```
[수정 후]
```
.authorizeHttpRequests(auth -> auth
                // 로그인, 회원가입 + 스웨거 관련 주소는 신분증 없이 프리패스!
                .requestMatchers(
                        "/user/signup", 
                        "/user/login",
                        "/swagger-ui/**", 
                        "/v3/api-docs/**", 
                        "/swagger-resources/**"
                ).permitAll()
                .anyRequest().authenticated() // 나머지는 다 신분증(JWT) 검사해!
        )
```



🚨 근데 잠깐! 스웨거에 '자물쇠'가 없네?!

이게 무슨 말이냐면, 로그인을 해서 'JWT 출입증'을 발급받아도, 지금 이 스웨거 메뉴판에는 그 출입증을 문지기한테 보여줄 구멍이 안 뚫려 있다는 뜻이야. 이대로 /api/ingredients에 재료를 추가하려고 하면 출입증을 못 내밀어서 또 403 거절을 당하게 돼!

스웨거한테 "우리 이제 출입증 검사하는 기능 생겼으니까, 출입증 넣는 버튼 좀 만들어줘!"라고 알려주는 설정 파일 추가 

스웨거 자물쇠(Authorize) 버튼 달아주기
```
package com.example.Naengbuhae.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT";
        
        // 1. 스웨거한테 "우리는 JWT라는 이름의 Bearer 토큰을 쓴다"고 알려주기
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer") // Bearer 토큰 방식
                        .bearerFormat("JWT")); // 토큰 형식은 JWT

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
```

---

## 🆕 이번 작업 정리 (프론트 스펙 정렬 + 신규 기능)

### 1) 레시피 ↔ 식재료 연결 + 매칭 추천 API

**무엇을 했나**
- 레시피마다 필요한 재료 목록을 등록할 수 있게 `RecipeIngredient` 엔티티 추가 (재료명, 필요 수량, 단위, **필수 여부**)
- 프론트 스펙에 맞춰 `Recipe` 엔티티를 전면 확장: `name`(기존 title), `category`, `difficulty`(easy/medium/hard), `servings`, `imageUrl`, `steps`(List), `nutrition`(@Embeddable: calories/protein/carbs/fat/sodium)
- 추천 API 신규: 내 냉장고 재료로 매칭률을 계산해 모든 레시피를 매칭률 내림차순으로 반환

**API**
```
GET /api/recipes/recommendations
응답: [{ recipe, matchRate, hasIngredients[], missingIngredients[] }, ...]
```

**매칭 규칙** (프론트 `matchRecipesWithIngredients`와 동일)
- 필수 재료(`required=true`)가 모두 있으면 → matchRate = 보유 / 전체 × 100
- 필수 재료가 하나라도 빠지면 → matchRate = 0
- 유통기한 지난 재료는 보유로 인정 안 함
- 재료명은 부분 매칭(양쪽 includes), 대소문자/공백 무시

**신규/수정 파일**
- 신규: `domain/RecipeIngredient.java`, `domain/Difficulty.java`, `domain/Nutrition.java`, `dto/RecipeIngredientDto.java`, `dto/NutritionDto.java`, `dto/RecipeMatchResponseDto.java`
- 수정: `domain/Recipe.java`, `dto/RecipeRequestDto.java`, `dto/RecipeResponseDto.java`, `repository/RecipeRepository.java`, `service/RecipeService.java`, `controller/RecipeController.java`

### 2) 내 프로필 조회/수정 API (`/user/me`)

**문제**: 프론트 `MyCustom` 페이지가 `localStorage.getItem('userProfile')`에서 데이터를 읽고 있어서, 다른 기기에서 로그인하면 빈 화면이 나옴. 백엔드 `/user/me`는 username만 echo back하던 상태.

**해결**
- `GET /user/me` → 로그인 사용자의 전체 프로필을 `UserResponseDto`로 반환
- `PUT /user/me` → 신체 정보 수정 + **권장 칼로리 자동 재계산** (Mifflin-St Jeor)
- `UserResponseDto`에 `recommendedCalories` 필드 추가

**API**
```
GET /user/me
응답: { id, username, name, gender, height, weight, birthDate, email,
        activityLevel, dietGoal, allergies, role, recommendedCalories }

PUT /user/me
요청: { name, gender, birthDate, height, weight,
        activityLevel, dietGoal, allergies }
응답: 업데이트된 UserResponseDto (재계산된 recommendedCalories 포함)
```

**신규/수정 파일**
- 신규: `user/ProfileUpdateRequest.java`
- 수정: `user/User.java`(updateProfile 메서드), `user/UserService.java`, `user/UserController.java`, `user/UserResponseDto.java`

### 3) 유통기한 임박 식재료 API

**API**
```
GET /api/ingredients/expiring?days=3   (기본 3일)
응답: [{ id, name, quantity, unit, category, storage,
         expirationDate, purchaseDate, daysLeft, status }, ...]
```
`status` 분류 (프론트 `getExpiryStatus`와 동일):
- `danger` : `daysLeft <= 0` (오늘 만료 + 이미 만료)
- `warning` : 1~3일 남음
- `safe` : 4일 이상

만료 임박순으로 자동 정렬됨.

**신규/수정 파일**
- 신규: `dto/ExpiringIngredientResponseDto.java`
- 수정: `service/IngredientService.java`, `controller/IngredientController.java`

### 4) 기본 레시피 시드 (8개 자동 등록)

**무엇을 했나**: 부팅 시 `recipe` 테이블이 비어있으면 프론트의 8개 하드코딩 레시피(`recipes.ts`)를 자동 등록. 소유자는 `system` 사용자(없으면 자동 생성, 비번은 추측 불가능한 UUID, 권한은 ADMIN).

**중복 방지**: 테이블이 비어있을 때만 시드 → 재부팅해도 중복 안 들어감.

**신규 파일**
- `config/RecipeSeeder.java`

---

## ⚠️ 부팅 전 필수 작업 — DB 마이그레이션

`spring.jpa.hibernate.ddl-auto=update`는 **컬럼 추가만** 자동이고 **이름 변경/제거는 안 됩니다**.
이번에 `recipe.title → name`, `instructions → steps`(별도 테이블) 등 구조가 바뀌어서 기존 테이블을 비워야 새 스키마가 정상 적용됩니다.

**Supabase SQL Editor에서 실행:**
```sql
DROP TABLE IF EXISTS recipe_ingredient CASCADE;
DROP TABLE IF EXISTS recipe_steps CASCADE;
DROP TABLE IF EXISTS recipe CASCADE;
```
재부팅하면 새 스키마가 자동 생성되고 시드 8개 레시피가 들어갑니다.

> 기존 레시피 데이터는 다 사라집니다. 어차피 컬럼 구조가 호환되지 않아 보존이 의미 없는 상태입니다.

---

## 📋 추가/변경된 엔드포인트 요약

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/recipes/recommendations` | 매칭률 기반 레시피 추천 |
| GET | `/api/ingredients/expiring?days=N` | 유통기한 N일 이내 임박 식재료 |
| GET | `/user/me` | 내 프로필 전체 조회 (응답 형식 변경) |
| PUT | `/user/me` | 내 프로필 수정 + 권장 칼로리 재계산 |

