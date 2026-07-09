# PrivateLog API 명세

베이스 URL: `https://<railway-domain>` (로컬: `http://localhost:8080`)

---

## 인증

JWT Bearer 토큰 방식을 사용한다.

```
Authorization: Bearer <token>
```

- 토큰은 `/auth/login` 또는 `/auth/register` 응답의 `token` 필드에서 획득
- 유효기간: 24시간

### 공개 엔드포인트 (토큰 불필요)
`GET /posts/**` (단, 비공개 게시글은 아래 규칙에 따라 제한됨), `POST /auth/register`, `POST /auth/login`, `GET /categories`,
`GET /posts/*/comments`, `POST /posts/*/comments`,
`PATCH /comments/**`, `DELETE /comments/**`

### 인증 필요 엔드포인트
`POST /auth/refresh`,
`POST /posts`, `PATCH /posts/**`, `DELETE /posts/**`,
`POST /categories`, `DELETE /categories/**`,
`GET /users`, `GET /users/**`, `DELETE /users/**`

---

## 비공개(isPublic=false) 게시글 노출 규칙

`GET /posts/**` 계열 엔드포인트는 요청에 유효한 `Authorization: Bearer <token>` 헤더가 있는지에 따라
비공개(`isPublic: false`) 게시글의 노출 여부가 갈린다.

| 구분 | 인증됨 (유효 토큰) | 미인증 |
|---|---|---|
| `GET /posts` | 전체(공개+비공개) 반환 | 공개 글만 반환 (비공개 글은 조용히 제외, 에러 없음) |
| `GET /posts/month/{yearMonth}` | 전체 반환 | 공개 글만 반환 (제외, 에러 없음) |
| `GET /posts/archive` | 전체 반환 | 공개 글만 반환 (제외, 에러 없음) |
| `GET /posts/{id}` | 항상 반환 | 비공개 글이면 **404** (`게시글을 찾을 수 없습니다.`) |
| `GET /posts/date/{date}` | 항상 반환 | 비공개 글이면 **404** (`해당 날짜의 게시글을 찾을 수 없습니다.`) |

단건 조회(`/posts/{id}`, `/posts/date/{date}`)는 403이 아니라 **404**를 반환한다.
글의 존재 여부 자체를 미인증 사용자에게 노출하지 않기 위함이다 (비공개 글이 "있는데 권한이 없다"는 정보도 주지 않음).

프론트엔드에서 로그인 상태로 비공개 글이 계속 안 보인다면, 위 GET 요청들에 `Authorization` 헤더를
빠짐없이 실어 보내고 있는지 먼저 확인할 것.

---

## 공통 에러 응답

```json
{ "message": "에러 설명 문자열" }
```

| HTTP 상태 | 발생 상황 |
|---|---|
| 400 | 입력값 오류, 중복, 권한 위반 |
| 404 | 리소스 없음 |
| 401 | 토큰 없음 또는 만료 |
| 500 | 서버 오류 |

---

## Auth

### POST /auth/register
회원가입. 성공 시 토큰 즉시 발급.

**Request Body**
```json
{
  "username": "minlee",
  "email": "min@example.com",
  "password": "password123",
  "nickname": "이민혁"
}
```
- `username`: 필수, 중복 불가
- `email`: 필수, 이메일 형식, 중복 불가
- `password`: 필수, 최소 8자
- `nickname`: 선택

**Response 200**
```json
{
  "token": "eyJhbGci...",
  "user": {
    "id": 1,
    "username": "minlee",
    "nickname": "이민혁"
  }
}
```

---

### POST /auth/login
로그인.

**Request Body**
```json
{
  "username": "minlee",
  "password": "password123"
}
```

**Response 200** — `/auth/register`와 동일 구조

---

### POST /auth/refresh
토큰 갱신. **인증 필요.** 현재 유효한(만료 전) 토큰으로 같은 사용자의 새 토큰을 재발급한다.

이미 만료된 토큰으로는 호출할 수 없다 (이 경우 다시 `/auth/login` 필요).

**Request Body** 없음

**Response 200** — `/auth/register`와 동일 구조

---

## Users

### GET /users
사용자 전체 조회. **인증 필요.**

**Response 200**
```json
[
  { "id": 1, "username": "minlee", "nickname": "이민혁" },
  { "id": 2, "username": "guest", "nickname": "게스트" }
]
```

---

### GET /users/{id}
사용자 조회. **인증 필요.**

**Response 200**
```json
{
  "id": 1,
  "username": "minlee",
  "nickname": "이민혁"
}
```

---

### DELETE /users/{id}
사용자 삭제. **인증 필요.**

**Response 204** (No Content)

---

## Categories

### GET /categories
카테고리 전체 조회.

**Response 200**
```json
[
  { "id": 1, "name": "일기" },
  { "id": 2, "name": "문화" },
  { "id": 3, "name": "기타" }
]
```

---

### POST /categories
카테고리 등록. **인증 필요.**

**Request Body**
```json
{ "name": "여행" }
```
- `name`: 필수, 최대 50자, 중복 불가

**Response 200**
```json
{ "id": 4, "name": "여행" }
```

**에러**
| HTTP 상태 | 발생 상황 |
|---|---|
| 400 | 이미 존재하는 카테고리 이름 |
| 401 | 토큰 없음 또는 만료 |

---

### DELETE /categories/{id}
카테고리 삭제. **인증 필요.** 해당 카테고리를 참조하는 게시글이 있으면 삭제 불가.

**Response 204** (No Content)

**에러**
| HTTP 상태 | 발생 상황 |
|---|---|
| 400 | 게시글이 존재하는 카테고리 삭제 시도 |
| 401 | 토큰 없음 또는 만료 |
| 404 | 카테고리 없음 |

---

## Posts

### GET /posts
게시글 페이징 목록. 카테고리 필터 선택 가능.
미인증 요청은 비공개(`isPublic: false`) 게시글이 목록에서 제외된다 (에러 아님 — 위 "비공개 게시글 노출 규칙" 참고).

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---|---|---|
| `categoryId` | 선택 | 카테고리 ID 필터 |
| `page` | 선택 | 페이지 번호 (기본 0) |
| `size` | 선택 | 페이지 크기 (기본 10) |
| `sort` | 선택 | 정렬 (기본 `createdAt,desc`) |

**Response 200**
```json
{
  "content": [
    {
      "id": 1,
      "title": "새해 첫 일기",
      "isPublic": true,
      "viewCount": 0,
      "categoryName": "일기",
      "authorNickname": "이민혁",
      "tags": [],
      "createdAt": "2026-01-01T00:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "number": 0,
  "size": 10
}
```

---

### GET /posts/{id}
게시글 단건 조회 (댓글 포함).

**Response 200**
```json
{
  "id": 1,
  "title": "새해 첫 일기",
  "content": "마크다운 본문...",
  "isPublic": true,
  "viewCount": 0,
  "category": { "id": 1, "name": "일기" },
  "author": { "id": 1, "username": "minlee", "nickname": "이민혁" },
  "tags": [],
  "comments": [...],
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-01-01T00:00:00"
}
```

**에러**
| HTTP 상태 | 발생 상황 |
|---|---|
| 404 | 게시글 없음, 또는 비공개 게시글을 미인증 상태로 조회 (위 "비공개 게시글 노출 규칙" 참고) |

---

### GET /posts/date/{date}
날짜로 게시글 단건 조회.

**Path** `date` 형식: `yyyy-MM-dd` (예: `2026-01-01`)

**Response 200** — `/posts/{id}`와 동일 구조

**에러**
| HTTP 상태 | 발생 상황 |
|---|---|
| 404 | 해당 날짜의 게시글 없음, 또는 비공개 게시글을 미인증 상태로 조회 |

---

### GET /posts/month/{yearMonth}
특정 월의 게시글 목록.
미인증 요청은 비공개 게시글이 목록에서 제외된다 (에러 아님).

**Path** `yearMonth` 형식: `yyyy-MM` (예: `2026-01`)

**Response 200**
```json
[
  {
    "id": 1,
    "title": "새해 첫 일기",
    "createdAt": "2026-01-01T00:00:00",
    ...
  }
]
```
`PostSummaryResponse` 배열 (페이징 없음)

---

### GET /posts/archive
연/월/일 계층 아카이브. 카테고리 필터 선택 가능.
미인증 요청은 비공개 게시글이 아카이브에서 제외된다 (에러 아님).

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---|---|---|
| `categoryId` | 선택 | 카테고리 ID 필터 |

**Response 200**
```json
[
  {
    "year": 2026,
    "months": [
      {
        "month": 2,
        "days": [
          {
            "date": "2026-02-15",
            "posts": [
              { "postId": 5, "title": "회사 이야기" }
            ]
          }
        ]
      }
    ]
  }
]
```

---

### POST /posts
게시글 등록. **인증 필요.**

**Request Body**
```json
{
  "title": "새해 첫 일기",
  "content": "마크다운 본문...",
  "isPublic": true,
  "categoryId": 1,
  "tagIds": []
}
```
- `title`: 필수, 최대 200자
- `content`: 필수
- `isPublic`: 필수 (true/false)
- `categoryId`: 선택
- `tagIds`: 선택 (생략 시 빈 배열)

**Response 200** — `/posts/{id}`와 동일 구조

---

### PATCH /posts/{id}
게시글 수정. **인증 필요.** 포함한 필드만 수정.

**Request Body** (모든 필드 선택)
```json
{
  "title": "수정된 제목",
  "content": null,
  "isPublic": null,
  "categoryId": null,
  "tagIds": null
}
```

**Response 200** — `/posts/{id}`와 동일 구조

---

### DELETE /posts/{id}
게시글 soft delete. **인증 필요.**

**Response 204** (No Content)

---

## Comments

### GET /posts/{postId}/comments
게시글의 댓글 트리 조회. 최상위 댓글만 반환하며 각 댓글의 `children`에 답글이 포함됨 (1단계).

**Response 200**
```json
[
  {
    "id": 1,
    "authorName": "이민혁",
    "content": "댓글 내용",
    "isSecret": false,
    "parentId": null,
    "children": [
      {
        "id": 2,
        "authorName": "익명",
        "content": "답글 내용",
        "isSecret": false,
        "parentId": 1,
        "children": [],
        "createdAt": "2026-01-01T12:00:00"
      }
    ],
    "createdAt": "2026-01-01T10:00:00"
  }
]
```

---

### POST /posts/{postId}/comments
댓글 등록. 로그인 사용자 또는 익명 모두 가능.

**Request Body**

로그인 사용자:
```json
{
  "content": "댓글 내용",
  "isSecret": false,
  "parentId": null
}
```

익명 사용자 (`authorName`, `password` 필수):
```json
{
  "content": "댓글 내용",
  "isSecret": false,
  "parentId": null,
  "authorName": "익명",
  "password": "1234"
}
```

답글 작성 시 `parentId`에 부모 댓글 ID 입력. 답글에 답글은 불가 (1단계 제한).

**Request Header** (로그인 시만): `Authorization: Bearer <token>`

**Response 200** — 단일 `CommentResponse`

---

### PATCH /comments/{id}
댓글 수정.

**Request Body**
```json
{
  "content": "수정된 내용",
  "password": "1234"
}
```
- 로그인 사용자: `password` 불필요, `Authorization` 헤더로 인증
- 익명 사용자: `password` 필수

**Response 200** — 단일 `CommentResponse`

---

### DELETE /comments/{id}
댓글 soft delete.

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---|---|---|
| `password` | 익명만 필수 | 익명 댓글 삭제 시 비밀번호 |

**Request Header** (로그인 시만): `Authorization: Bearer <token>`

**Response 204** (No Content)

---

## CORS 설정 안내

React 개발 서버(`localhost:5173`)에서 이 API를 호출하려면
백엔드 `SecurityConfig` 또는 별도 `CorsConfig`에 아래 설정이 필요하다.

```java
// SecurityConfig.filterChain() 내에 추가
http.cors(cors -> cors.configurationSource(request -> {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173", "https://<프론트엔드-도메인>"));
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return config;
}));
```
