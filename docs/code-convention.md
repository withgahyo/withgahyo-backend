# Code Convention

백엔드 코드를 일관되게 작성하기 위한 최소 규칙입니다. 새 기능을 구현할 때는 이 문서를 기준으로 이름, 패키지, 계층 책임, 예외 처리 방식을 맞춥니다.

## 기본 원칙

- 읽는 사람이 의도를 바로 알 수 있는 코드를 작성합니다.
- 불필요한 추상화는 만들지 않습니다.
- 중복 제거보다 명확성을 우선합니다.
- 한 메서드는 하나의 책임만 가지도록 작성합니다.
- API, ERD, 공통 응답 규칙과 충돌하면 문서를 먼저 확인합니다.

## Naming Convention

### 공통

- 클래스명은 `PascalCase`를 사용합니다.
- 메서드명과 변수명은 `camelCase`를 사용합니다.
- 상수는 `UPPER_SNAKE_CASE`를 사용합니다.
- 패키지명은 소문자 단수형을 사용합니다.
- 이름은 축약하지 않고 의미가 드러나게 작성합니다.
- 한 글자 변수명은 반복문 인덱스처럼 범위가 아주 좁은 경우에만 사용합니다.

```java
private static final String SUCCESS_CODE = "COMMON_SUCCESS";

private Long courseId;
private String nickname;
private List<Course> courses;
```

### Boolean

boolean 값은 `is`, `has`, `can`, `should` 중 하나로 시작합니다.

```java
private boolean isRead;
private boolean hasNext;
private boolean canEdit;
private boolean shouldRefresh;
```

### Date/Time

날짜와 시간은 의미에 맞게 suffix를 사용합니다.

```java
private LocalDate startDate;
private LocalTime arrivalTime;
private LocalDateTime createdAt;
```

## Static Factory Method Naming

| 이름 | 사용 기준 |
| --- | --- |
| `of` | 여러 값을 조합해 값 객체나 단순 객체를 만들 때 |
| `from` | 하나의 source 객체에서 DTO나 객체를 만들 때 |
| `toEntity` | Request DTO를 Entity로 변환할 때 |
| `create` | 도메인 규칙이 있는 Entity를 생성할 때 |
| `updateXxx` | 기존 Entity 상태를 변경할 때 |
| `toResponse` | 지양 |

```java
CoursePeriod.of(startDate, endDate);
CourseSummaryResponse.from(course);
request.toEntity(user, region);
Course.create(user, region, request);
course.updateBasicInfo(title, startDate, endDate);
```

`toResponse`는 가급적 사용하지 않습니다. Entity가 API 응답 DTO를 알게 되면 의존 방향이 꼬이므로, 응답 DTO에서 `from(entity)`로 변환합니다.

## Package Convention

도메인 코드는 도메인별 패키지에 둡니다.

```text
com.withgahyo.domain.course
 ├── controller
 ├── service
 ├── repository
 ├── entity
 ├── dto
 └── exception
```

- 공통 설정, 응답, 예외는 `global`에 둡니다.
- 외부 API, 파일 저장소, AI 서버 연동은 `infra`에 둡니다.
- 도메인별 에러 코드는 해당 도메인 패키지 내부에 둡니다.

```text
com.withgahyo.domain.course.exception.CourseErrorCode
com.withgahyo.domain.album.exception.AlbumErrorCode
```

## Layer Convention

### Controller

- 비즈니스 로직을 작성하지 않습니다.
- 요청 검증은 DTO와 Bean Validation을 사용합니다.
- 데이터가 있는 성공 응답은 `ApiResponse.success(data)`로 감쌉니다.
- 데이터가 없는 성공 응답은 `ApiResponse.ok()`로 감쌉니다.
- path와 HTTP method는 `docs/api-design.md`를 따릅니다.

### Service

- 유스케이스와 트랜잭션 경계를 담당합니다.
- 조회 전용 메서드는 `@Transactional(readOnly = true)`를 사용합니다.
- 변경 메서드는 `@Transactional`을 사용합니다.
- 여러 테이블을 함께 변경하는 API는 하나의 트랜잭션으로 처리합니다.
- 도메인 검증 실패는 `BusinessException`으로 처리합니다.
- 다른 도메인의 데이터가 필요하면 해당 도메인의 공개 Service 메서드를 우선 사용합니다.
- 단순 존재 확인이나 FK 검증처럼 현재 유스케이스 내부의 DB 조회가 명확한 경우에는 Repository를 직접 주입할 수 있습니다.
- 다른 도메인의 상태 변경은 해당 도메인 Service를 통해 처리합니다.

### Repository

- 단순 조회는 Spring Data JPA 메서드명을 사용합니다.
- 조건이 많아 메서드명이 길어지면 JPQL 또는 별도 query 구조를 검토합니다.
- Controller에서 Repository를 직접 호출하지 않습니다.

## DTO Convention

- Request DTO와 Response DTO를 분리합니다.
- Entity를 그대로 응답으로 반환하지 않습니다.
- DTO는 가능하면 `record`를 사용합니다.
- Request DTO에는 Bean Validation을 작성합니다.

```java
CreateCourseRequest
UpdateCourseRequest
CourseDetailResponse
CourseSummaryResponse
```

## Entity Convention

- Entity에는 setter를 지양합니다.
- 생성자는 `protected` 기본 생성자를 사용합니다.
- 생성은 정적 팩토리 메서드 또는 의미 있는 생성 메서드를 사용합니다.
- 변경은 setter 대신 `updateXxx`, `complete`, `cancel`처럼 의미 있는 메서드로 작성합니다.
- ERD 기준 필드명과 타입을 우선 따릅니다.
- Java 필드는 `camelCase`, DB 컬럼은 ERD의 `snake_case`를 사용합니다.
- 엔티티 필드에는 `@Column(name = "snake_case")`를 명시해 ERD 컬럼과 매핑을 분명히 합니다.
- `deleted_at`이 있는 테이블은 soft delete 기준으로 처리합니다.

## Exception Convention

- 비즈니스 예외는 `BusinessException`을 사용합니다.
- 에러 코드는 `ErrorCode` 인터페이스를 구현한 enum으로 정의합니다.
- 공통 에러는 `CommonErrorCode`, 인증/인가 에러는 `SecurityErrorCode`를 사용합니다.
- `SecurityErrorCode`는 보안 영역의 클래스명이지만, 응답 코드 prefix는 클라이언트가 이해하기 쉬운 인증/인가 도메인 의미로 `AUTH_`를 유지합니다.
- 도메인 에러는 도메인 패키지 내부에 둡니다.
- 에러 코드 형식은 `{도메인}_{HTTP상태}_{일련번호}`를 따릅니다.

## Enum Convention

- DB에는 문자열로 저장합니다.
- JPA에서는 `@Enumerated(EnumType.STRING)`을 사용합니다.
- enum 이름은 ERD CHECK 값과 동일한 대문자 형식을 사용합니다.

```java
CourseStatus.DRAFT
CourseStatus.GENERATING
CourseStatus.COMPLETED
```

## Delete Convention

- `deleted_at`이 있는 테이블은 soft delete를 기본으로 합니다.
- `deleted_at`이 없는 테이블은 삭제 API를 만들기 전에 ERD 변경 여부를 먼저 확인합니다.
- 예외적으로 `review`는 `deleted_at`이 없고 삭제 API가 필요하므로 hard delete로 처리합니다.
- soft delete 대상 조회 시 삭제된 데이터는 기본적으로 제외합니다.

## Test Convention

- 기능 구현 시 Service 테스트를 우선 작성합니다.
- Controller 테스트는 API 계약 검증이 중요할 때 작성합니다.
- 테스트명은 동작과 기대 결과가 드러나게 작성합니다.

```java
createCourse_success()
createCourse_fail_whenRegionNotFound()
```

문서만 수정한 경우 테스트는 생략할 수 있습니다.
