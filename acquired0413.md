# 2026-04-13 학습 내용 정리

---

## Java `record` 키워드

- Java 16에서 정식 도입된 키워드 (14~15는 preview)
- 불변 데이터 클래스를 위한 boilerplate 자동 생성
- 컴파일러가 생성자, getter, equals, hashCode, toString을 자동으로 만들어줌
- getter 이름이 일반 class와 다름: `getId()` 대신 `id()`
- Jackson은 record의 getter 명명 규칙을 자동 인식해서 별도 설정 없이 JSON 변환 가능
- 중괄호 안에 compact constructor(유효성 검사), static 팩토리 메서드, 인스턴스 메서드 추가 가능
- 인스턴스 필드 추가 및 필드 값 변경(setter)은 불가
- Lombok의 `@Value`와 역할이 거의 같으나 record는 언어 자체에 내장

---

## Spring 컨트롤러 응답 방식

| 방법 | 특징 |
|------|------|
| Entity 직접 반환 | 민감 필드 노출, 순환 참조 위험 — 비권장 |
| DTO | 가장 일반적이고 안전한 방법 |
| ResponseEntity<T> | HTTP 상태코드, 헤더를 직접 제어 가능 |
| Projection (Spring Data JPA) | 인터페이스만 정의하면 필요한 컬럼만 SELECT |
| Map<String, Object> | 타입 안정성 없어 유지보수 어려움 — 비권장 |

---

## PostgreSQL + Hibernate 6의 `@Lob` 문제

- Hibernate 6(Spring Boot 3+)부터 `@Lob String`을 PostgreSQL에서 `TEXT`가 아닌 `oid`(large object)로 매핑
- `oid`는 별도 large object storage를 사용해 일반 문자열처럼 쿼리하기 불편하고 오동작 가능
- 해결책: `@Column(columnDefinition = "TEXT")` 사용
- `columnDefinition`은 PostgreSQL 종속적이지만 실제 동작이 정확함
- DB 독립성을 유지하려면 `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)` 사용 (Hibernate 6 권장 방식)

---

## Gradle 의존성 스코프와 테스트 classpath

- `runtimeOnly`는 메인 소스 실행 시에만 포함되며 `testRuntimeClasspath`에 포함되지 않음
- 테스트에서 드라이버가 필요하면 `testRuntimeOnly`를 별도로 추가해야 함
- `testRuntimeClasspath` 구성: `testImplementation` + `testRuntimeOnly`
- build.gradle 수정 후 IDE에서 Gradle 프로젝트 리로드를 해야 변경사항이 classpath에 반영됨

---

## 테스트 전용 application.yaml

- `src/test/resources/application.yaml`을 만들면 테스트 실행 시 메인 설정을 덮어씀
- 테스트용으로 H2 인메모리 DB를 사용하면 실제 DB 없이 테스트 가능
- IDE에서 Run(main) → `src/main/resources/application.yaml` (PostgreSQL)
- IDE에서 Test 실행 → `src/test/resources/application.yaml` (H2)

---

## `@SQLRestriction`과 soft delete

- `@SQLRestriction("deleted_at IS NULL")` — 해당 Entity의 모든 JPA 조회에 자동으로 WHERE 조건 추가
- soft delete: 실제 삭제 없이 `deleted_at` 필드에 값을 세팅하는 방식
- `@SQLRestriction`을 우회해 삭제된 데이터도 조회하려면 native query 사용
- soft delete 후 같은 트랜잭션 안에서 동일 id 재조회 시 JPA 1차 캐시에서 반환 → `@SQLRestriction` 미적용 (주의)
- 1차 캐시 문제가 걱정되면 `EntityManager.clear()`로 캐시 비우기 가능하나 소규모엔 과설계

### soft delete 로직 위치 고민

- Entity에 `delete()` 메서드를 두면 `@SQLRestriction`과 일관성 있음
- Service에서 처리하면 "데이터 구조(Entity) vs 행위(Service)" 역할 분리
- 정답은 없으며 트레이드오프가 있음. 중요한 것은 팀/프로젝트 내 일관성

---

## 카테고리 초기 데이터 — DataInitializer

- `ApplicationRunner`를 구현하면 Spring Boot 기동 시 자동 실행
- `count() == 0` 조건으로 서버 재시작 시 중복 삽입 방지
- 카테고리처럼 자주 바뀌지 않는 고정 데이터를 초기화하는 용도로 적합
- 개인 블로그 수준에서는 기본 카테고리를 기동 시 삽입하고, 관리자 API로 추가/삭제하는 방식이 현실적

---

## API 설계 — 게시글과 댓글 함께 vs 분리 반환

| | 함께 반환 | 분리 반환 |
|--|---------|---------|
| 클라이언트 요청 | 1번 | 2번 |
| 댓글 페이징 | 어려움 | 용이 |
| 응답 무게 | 댓글 많으면 무거움 | 가벼움 |
| 적합한 상황 | 소규모 블로그 | 댓글이 많은 서비스 |

---

## API 응답 형식 — JSON DTO vs 마크다운 직접 출력

- API는 데이터를 주는 역할(JSON DTO)에 집중하고, 표현은 프론트엔드가 담당하는 게 좋음
- 마크다운을 직접 반환하면 표현 방식 변경 시 서버 코드 수정 필요
- 마크다운 export 전용 엔드포인트는 별도로 두는 건 괜찮음 (용도를 혼용하지 않는 게 핵심)

---

## 프론트엔드 호스팅 방식

| | 같은 서버 호스팅 | 분리 호스팅 |
|--|--------------|-----------|
| 구성 | Spring Boot static/ 폴더에 HTML+JS 배치 | 별도 서버/서비스 |
| CORS | 불필요 | 필요 |
| 배포 | WAR 하나로 해결 | 각각 별도 배포 |
| 적합한 상황 | 초기, 소규모 | 프론트 프레임워크 사용 시 |

---

## 호스팅 서비스 비교

### PaaS (Platform as a Service)
서버 관리 없이 코드/바이너리만 올리면 플랫폼이 실행해주는 서비스

| 서비스 | 특징 |
|--------|------|
| Railway | 월 $5 크레딧 제공, GitHub 연동 자동 배포, DB 함께 운영 가능 |
| Render | 무료 플랜 있으나 15분 비활성 시 슬립, 유료 $7/월 |
| Supabase | PostgreSQL 전용, 무료 500MB, 1주일 비활성 시 일시정지 |
| Neon | PostgreSQL 전용, 무료 0.5GB, 자동 스케일-투-제로 |

### IaaS (Infrastructure as a Service) = VPS
Linux 서버 자체를 임대하는 방식. 설치/설정을 직접 해야 함

| 서비스 | 특징 |
|--------|------|
| Oracle Cloud Free Tier | ARM VM 2개, 4코어/24GB RAM 영구 무료 |
| Vultr / DigitalOcean | 월 $6~12, 자유도 높음 |

### cafe24
- 웹호스팅 플랜: Spring Boot 실행 불가 (PHP 기반 환경)
- VPS 플랜: 가능하나 비용 대비 Railway/Oracle Free Tier가 유리

---

## VPS 구성 시 준비 사항

Spring Boot WAR 외에 추가로 준비하는 것들:

```
인터넷 → Nginx (리버스 프록시) → Spring Boot (8080) → PostgreSQL
```

| 구성 요소 | 역할 |
|----------|------|
| Nginx | 80/443 포트 요청을 8080으로 전달, SSL 처리, 정적 파일 서빙 |
| Let's Encrypt (certbot) | 무료 SSL 인증서 발급/자동 갱신 |
| systemd 서비스 등록 | 서버 재시작 시 Spring Boot 자동 실행 |
| 도메인 (선택) | IP 대신 주소로 접속, SSL 발급에도 필요 |

---

## SSL 인증서와 프론트/백엔드 분리

- 프론트엔드가 HTTPS라면 백엔드도 HTTPS여야 함
- **Mixed Content 정책**: 브라우저가 HTTPS 페이지에서 HTTP 리소스 호출을 차단
- 분리 호스팅 시 프론트/백엔드 모두 SSL 필요
- 같은 서버에서 호스팅하면 Nginx에 인증서 하나만 적용하면 됨

---

## 도메인과 DNS

- **DNS(Domain Name System)**: 도메인 이름을 IP 주소로 변환하는 시스템
- **A 레코드**: 도메인 → IP 주소 연결에 쓰이는 DNS 설정 항목
- 도메인 구매 서비스: Namecheap, Cloudflare Registrar (저렴), 가비아 (한국)
- **Cloudflare**: 도메인 구매 + DNS 관리 + DDoS 방어 + HTTPS 프록시를 무료 플랜으로 제공
- 당장 필요하지 않으며, PaaS 서비스는 임시 도메인을 자동으로 제공함
