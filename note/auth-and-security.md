# 인증 및 보안 구조 노트

## 개요

Spring Security 7 + JJWT 0.12.x 기반의 Stateless JWT 인증을 사용한다.
세션을 유지하지 않으며, 매 요청마다 HTTP 헤더에 포함된 토큰을 검증한다.

---

## JWT 토큰 구조

### 발급 시점
- `POST /auth/register` — 회원가입 완료 시
- `POST /auth/login` — 로그인 성공 시

### 토큰 내 Claims

| 필드 | 값 | 설명 |
|---|---|---|
| `sub` (subject) | `userId` (Long → String) | 사용자 PK |
| `username` | String | 로그인 아이디 |
| `role` | `"USER"` 또는 `"ADMIN"` | 권한 |
| `iat` | timestamp | 발급 시각 |
| `exp` | timestamp | 만료 시각 (기본 24시간) |

### 서명 알고리즘
- HS256 (HMAC-SHA256)
- 키는 `application.yaml`의 `jwt.secret` (UTF-8 바이트, 최소 32바이트 필요)

---

## 요청 흐름

```
클라이언트                          서버
   |                                  |
   |  POST /auth/login                |
   |  { username, password }  ──────► |  UsersRepository.findByUsername()
   |                                  |  PasswordEncoder.matches()
   |  ◄── { token, user }             |  JwtProvider.generateToken()
   |                                  |
   |  GET /posts  (공개)      ──────► |  JwtAuthenticationFilter (토큰 없어도 통과)
   |  ◄── posts[]                     |
   |                                  |
   |  POST /posts                     |
   |  Authorization: Bearer <token>   |
   |                          ──────► |  JwtAuthenticationFilter
   |                                  |  → 토큰 파싱 → AuthUser → SecurityContext 설정
   |                                  |  → SecurityConfig: 인증 확인 통과
   |                                  |  → PostController: @AuthenticationPrincipal AuthUser
   |  ◄── 생성된 post                 |
```

---

## SecurityConfig — 엔드포인트 접근 제어

### 공개 (인증 없이 접근 가능)

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/auth/**` | 회원가입, 로그인 |
| GET | `/posts/**` | 게시글 전체 조회 |
| ALL | `/posts/*/comments/**` | 댓글 조회/작성 (익명 허용) |
| ALL | `/comments/**` | 댓글 수정/삭제 (서비스에서 권한 검증) |
| GET | `/`, `/index.html`, `/css/**`, `/js/**` | 정적 리소스 |

### 인증 필요

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/posts` | 게시글 작성 |
| PATCH | `/posts/**` | 게시글 수정 |
| DELETE | `/posts/**` | 게시글 삭제 |
| GET | `/users/{id}` | 사용자 조회 |
| DELETE | `/users/{id}` | 사용자 삭제 |

---

## 댓글 권한 처리 방식

댓글은 두 가지 작성 방식을 지원하므로 Spring Security에서 일괄 처리하지 않고
Service 레이어에서 직접 권한을 검증한다.

| 구분 | 작성 조건 | 삭제/수정 조건 |
|---|---|---|
| 로그인 사용자 | 토큰 포함 요청 | 토큰의 userId == 댓글 작성자 userId |
| 익명 사용자 | authorName + password 입력 | password 일치 여부 확인 |

컨트롤러에서 `authUser`가 null이면 익명으로 간주하고 서비스에 `userId = null`을 전달한다.

---

## AuthUser — 컨트롤러 Principal

```java
public record AuthUser(Long id, String username, UserRole role) {}
```

`JwtAuthenticationFilter`가 토큰을 파싱한 뒤 이 객체를 `Authentication.getPrincipal()`에 설정한다.
컨트롤러에서 `@AuthenticationPrincipal AuthUser authUser`로 주입받아 사용한다.

공개 엔드포인트에서 토큰이 없는 경우 `authUser`는 `null`이다.
(`@AuthenticationPrincipal`은 principal 타입이 `AuthUser`가 아니면 null을 주입)

---

## 로그아웃

서버 측에는 로그아웃 엔드포인트가 없다. Stateless JWT의 특성상 서버가 토큰 상태를 관리하지 않는다.

**프론트엔드 처리 방법:**
1. 로그인 시 발급받은 토큰을 `localStorage` 또는 `sessionStorage`에 저장
2. 로그아웃 시 저장된 토큰을 삭제
3. 이후 요청에 토큰이 없으므로 서버는 인증 실패(401)로 처리

**토큰 만료:** `jwt.expiration` 설정값(기본 86400000ms = 24시간) 경과 후 자동 무효화

---

## 토큰 무효화 고도화 방안 (미구현)

현재 구조에서는 발급된 토큰을 만료 전에 강제 무효화할 수 없다.
필요하다면 아래 두 가지 방법을 고려할 수 있다.

### A. Refresh Token 분리
- Access Token: 짧은 유효기간 (15분~1시간)
- Refresh Token: 긴 유효기간 (7~30일), DB에 저장
- 로그아웃 시 Refresh Token을 DB에서 삭제 → Access Token 만료 후 갱신 불가
- Access Token 만료 시 클라이언트가 Refresh Token으로 재발급 요청

### B. 블랙리스트
- 로그아웃 요청 시 해당 토큰을 DB 또는 Redis에 저장
- `JwtAuthenticationFilter`에서 블랙리스트 조회 후 차단
- 토큰 만료 시각이 지나면 블랙리스트 항목 자동 제거 가능

개인 블로그 특성상 사용자가 1명이므로 현재 구현으로 충분하다.

---

## 설정값

```yaml
# application.yaml
jwt:
    secret: privatelog-jwt-secret-key-for-hs256-algorithm-minimum-256bits
    expiration: 86400000   # 24시간 (밀리초)
```

secret은 HS256 기준 최소 32바이트(256비트) 이상이어야 한다.
프로덕션 배포 시 환경변수로 주입하는 것을 권장한다.

```
# Railway 환경변수 예시
JWT_SECRET=your-production-secret-key
```

`application.yaml`에서 `${JWT_SECRET:기본값}` 형태로 참조하도록 변경하면 된다.
