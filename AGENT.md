# PrivateLog — 개발 규칙 및 기술 스택

개인 일기/블로그 서비스. Spring Boot 기반 REST API 서버.

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.x (WAR 배포) |
| ORM | Spring Data JPA / Hibernate 7 |
| DB | PostgreSQL 18 |
| 인증 | Spring Security 7 + JWT (JJWT 0.12.x) |
| 빌드 | Gradle |
| 유틸 | Lombok |
| 테스트 DB | H2 (in-memory) |

---

## 패키지 구조

```
com.maylee.privatelog
├── controller      # REST 컨트롤러
├── service         # 비즈니스 로직
├── repository      # Spring Data JPA Repository
├── entity          # JPA 엔티티
├── security        # JWT, Spring Security 설정
├── dto
│   ├── post
│   ├── comment
│   ├── category
│   ├── tag
│   └── user
└── DataInitializer # 애플리케이션 기동 시 초기 데이터 삽입
```

---

## 엔티티 규칙

- 모든 엔티티는 `BaseTimeEntity`를 상속해 `createdAt`, `updatedAt`을 공통으로 가진다.
- 생성자는 `@NoArgsConstructor` + `@Builder`를 함께 사용한다. (`@Builder`는 `@AllArgsConstructor`를 내부 생성하므로 명시 불필요)
- `Users`는 예외적으로 `@Builder`를 사용하지 않고 `static register()` 팩토리 메서드로 생성한다.
- 필드는 모두 `private`이며 `@Getter`로 노출한다. setter는 사용하지 않는다.
- 상태 변경은 엔티티 내부 메서드로만 처리한다. (예: `post.delete()`, `post.update(...)`)
- soft delete가 필요한 엔티티는 `deletedAt` 필드와 `@SQLRestriction("deleted_at IS NULL")`을 사용한다.
- DB 종속 컬럼 타입은 `columnDefinition`으로 명시한다. (예: `columnDefinition = "TEXT"`)
- `@Lob`은 사용하지 않는다. PostgreSQL에서 `oid` 타입으로 매핑되는 문제가 있다.
- 연관관계 fetch 전략은 기본적으로 `LAZY`를 사용한다.

---

## DTO 규칙

- DTO는 Java `record`로 선언한다.
- 응답 DTO는 `XxxResponse`, 요청 DTO는 `XxxRequest`로 네이밍한다.
- 엔티티 → DTO 변환은 `record` 내부의 `static from()` 메서드로 처리한다.
- 엔티티를 컨트롤러 응답으로 직접 반환하지 않는다.

---

## Repository 규칙

- `@SQLRestriction`을 우회해야 하는 조회는 native query를 사용한다.
- hard delete는 native query `@Modifying @Query`로 처리한다.
- JPQL과 native query를 혼용할 때는 쿼리 위에 주석으로 용도를 명시한다.

---

## Service 규칙

- 클래스 레벨에 `@Transactional(readOnly = true)`를 선언하고, 쓰기 메서드에만 `@Transactional`을 추가한다.
- 엔티티 조회 실패 시 `NoSuchElementException`을 사용한다.
- 권한 위반 시 `IllegalArgumentException`을 사용한다.
- 비즈니스 규칙 검증(예: 대댓글 1단계 제한)은 Service에서 처리한다.

---

## Controller 규칙

- 모든 응답은 `ResponseEntity<T>`로 감싼다.
- 요청 DTO 검증은 `@Valid`를 사용한다.
- 인증이 필요한 엔드포인트는 `@AuthenticationPrincipal AuthUser authUser`로 현재 사용자를 주입받는다.
- 댓글처럼 익명과 인증 모두 허용하는 엔드포인트는 `authUser`가 null일 수 있으므로 null 체크 후 Service에 전달한다.
- 날짜 파라미터는 `@DateTimeFormat`으로 포맷을 명시한다.

---

## Security 규칙

- 인증 방식: Stateless JWT. 세션을 사용하지 않는다.
- 토큰은 `Authorization: Bearer <token>` 헤더로 전달한다.
- `JwtAuthenticationFilter`가 매 요청마다 토큰을 파싱해 `SecurityContextHolder`에 `AuthUser`를 설정한다.
- `AuthUser(Long id, String username, UserRole role)`가 컨트롤러의 Principal이 된다.
- 공개 엔드포인트: `GET /posts/**`, `POST /auth/**`, `/posts/*/comments/**`, `/comments/**`, 정적 리소스
- 인증 필요 엔드포인트: `POST /posts`, `PATCH /posts/**`, `DELETE /posts/**`, `GET/DELETE /users/**`
- 서버 측 로그아웃 처리는 없다. 클라이언트가 토큰을 삭제하는 것으로 로그아웃을 처리한다.

---

## DB / 인프라 규칙

- DDL 스크립트는 `src/main/resources/static/schema.sql`에 관리한다.
- `application.yaml`의 `ddl-auto`는 `validate`를 사용한다. (Hibernate가 스키마를 자동 변경하지 않음)
- 카테고리 초기 데이터는 `DataInitializer`에서 애플리케이션 기동 시 삽입한다. (`count() == 0` 조건으로 중복 방지)
- 테스트 환경은 `src/test/resources/application.yaml`에서 H2로 오버라이드한다.

---

## 프론트엔드

- 별도 프레임워크 없이 순수 **HTML + JavaScript**로 구현한다.
- 파일은 `src/main/resources/static/`에 위치시켜 Spring Boot가 직접 서빙한다.
- 백엔드와 동일한 서버에서 호스팅하므로 CORS 설정은 불필요하다.
- 백엔드 API를 `fetch()`로 호출해 데이터를 렌더링하는 방식으로 구현한다.
- 로그아웃은 클라이언트 측에서 저장된 토큰을 삭제하는 방식으로 처리한다.

---

## 미구현 / 추후 예정

- `CategoryService` — 카테고리 관리 API
- 태그 필터링 기반 게시글 목록 조회
- Refresh Token 기반 토큰 갱신 (필요 시)

## 코드 수정 지침

AI는 작업 수행시 이 문서를 읽고 작업을 수행하며, 종료 후 다시 이 문서를 읽고 필요에 따라 업데이트를 해주기 바랍니다.