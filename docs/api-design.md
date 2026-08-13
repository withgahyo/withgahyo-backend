# API 설계

이 문서는 백엔드 개발 에이전트가 컨트롤러, 서비스, DTO를 만들 때 기준으로 삼는 API 목록입니다. 세부 request/response 필드는 기능 구현 시 Notion API 명세와 화면 요구사항에 맞춰 보강합니다.

## 기본 규칙

| 항목 | 값 |
| --- | --- |
| Base URL | `https://api.gatigahyo.com` |
| Base Path | `/api/v1` |
| Content-Type | `application/json` |
| 인증 헤더 | `Authorization: Bearer {accessToken}` |

삭제 API는 별도 명시가 없으면 soft delete를 기본으로 합니다.
(예외: `review`는 `deleted_at` 컬럼이 없어 hard delete로 처리합니다.)

## 공통 성공 응답

성공 응답은 별도 `SuccessCode` enum을 만들지 않고, `ApiResponse` 내부 상수로 관리합니다. 현재 백엔드 코드도 `global.response.ApiResponse` 안의 `SUCCESS_CODE`, `SUCCESS_MESSAGE` 상수를 사용합니다.

성공 코드는 `COMMON_SUCCESS` 하나만 사용합니다. 에이전트는 `SuccessCode` enum을 새로 만들거나, 성공 응답 코드를 도메인별로 분리하지 않습니다.

```json
{
  "success": true,
  "code": "COMMON_SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

데이터가 없는 성공 응답은 `data`에 `null`을 내려줍니다.

```json
{
  "success": true,
  "code": "COMMON_SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": null
}
```

## 공통 실패 응답

실패 응답은 `ErrorResponse`를 사용합니다.

```json
{
  "success": false,
  "timestamp": "2026-08-13T12:00:00",
  "status": 400,
  "code": "COMMON_400_001",
  "message": "잘못된 입력값입니다.",
  "path": "/api/v1/example",
  "errors": [
    {
      "field": "nickname",
      "reason": "닉네임은 필수입니다."
    }
  ]
}
```

`errors`는 Bean Validation 실패처럼 필드 단위 원인을 내려줘야 할 때 사용합니다. 일반 비즈니스 예외에서는 빈 배열로 내려갈 수 있습니다.

## 에러 코드 규칙

에러 코드는 `{도메인}_{HTTP상태}_{일련번호}` 형식을 사용합니다.

| 항목 | 규칙 |
| --- | --- |
| 형식 | `{도메인}_{HTTP상태}_{일련번호}` |
| 예시 | `COMMON_400_001`, `AUTH_401_001`, `COURSE_404_001` |
| 순번 기준 | 등록 순서대로 채번 |
| 번호 범위 | 도메인 + HTTP 상태 조합별로 독립 관리 |
| 삭제된 번호 | 재사용하지 않음 |
| 도메인 prefix | 대문자 단수형 사용 |

현재 공통으로 정의된 코드는 다음과 같습니다.

| 코드 | HTTP 상태 | 메시지 |
| --- | --- | --- |
| `COMMON_400_001` | 400 | 잘못된 입력값입니다. |
| `COMMON_400_002` | 400 | 요청 파라미터 형식이 올바르지 않습니다. |
| `COMMON_405_001` | 405 | 지원하지 않는 HTTP 메서드입니다. |
| `COMMON_500_001` | 500 | 서버 내부 오류가 발생했습니다. |
| `AUTH_401_001` | 401 | 인증이 필요합니다. |
| `AUTH_401_002` | 401 | 유효하지 않은 토큰입니다. |
| `AUTH_403_001` | 403 | 접근 권한이 없습니다. |

도메인별 에러 코드는 해당 도메인 패키지 안에서 enum으로 추가합니다.

예시:

```text
com.withgahyo.domain.course.exception.CourseErrorCode
com.withgahyo.domain.album.exception.AlbumErrorCode
```

## API 목록

### 회원/인증

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login/kakao` | 불필요 | 카카오 로그인 |
| `POST` | `/api/v1/auth/login/google` | 불필요 | 구글 로그인 |
| `POST` | `/api/v1/auth/reissue` | 불필요 | access token 재발급 |
| `POST` | `/api/v1/auth/logout` | 필요 | 로그아웃 |
| `GET` | `/api/v1/users/me` | 필요 | 내 정보 조회 |
| `PATCH` | `/api/v1/users/me` | 필요 | 내 프로필 수정 |
| `DELETE` | `/api/v1/users/me` | 필요 | 회원 탈퇴 |

### 온보딩/가족 구성원

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/onboarding` | 필요 | 온보딩 진행 상태 조회 |
| `PUT` | `/api/v1/onboarding` | 필요 | 온보딩 정보 저장 또는 수정 |
| `POST` | `/api/v1/onboarding/complete` | 필요 | 온보딩 완료 처리 |
| `GET` | `/api/v1/family-members` | 필요 | 가족 구성원 목록 조회 |
| `POST` | `/api/v1/family-members` | 필요 | 가족 구성원 등록 |
| `GET` | `/api/v1/family-members/{familyMemberId}` | 필요 | 가족 구성원 상세 조회 |
| `PATCH` | `/api/v1/family-members/{familyMemberId}` | 필요 | 가족 구성원 정보 수정 |
| `DELETE` | `/api/v1/family-members/{familyMemberId}` | 필요 | 가족 구성원 삭제 |
| `PUT` | `/api/v1/family-members/{familyMemberId}/preferences` | 필요 | 가족 구성원 관광/음식/이동 선호 저장 |
| `GET` | `/api/v1/family-members/{familyMemberId}/preferences` | 필요 | 가족 구성원 선호 조회 |

`PUT /api/v1/family-members/{familyMemberId}/preferences`는 하나의 요청으로 가족 구성원의 선호 정보를 전체 교체합니다. 구현 시 하나의 트랜잭션 안에서 `family_member`의 이동/음식 관련 컬럼을 수정하고, `family_member_tourism_preference`, `family_member_facility` 매핑을 삭제 후 재삽입합니다.

### 홈

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/home` | 필요 | 홈 화면 요약 조회 |
| `GET` | `/api/v1/home/courses/upcoming` | 필요 | 다가오는 코스 목록 조회 |
| `GET` | `/api/v1/home/albums/recent` | 필요 | 최근 앨범 목록 조회 |
| `GET` | `/api/v1/home/notifications/recent` | 필요 | 최근 알림 목록 조회 |

### 지역/장소/선택지

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/regions` | 필요 | 지역 목록 조회 |
| `GET` | `/api/v1/places` | 필요 | 장소 검색 |
| `GET` | `/api/v1/places/{placeId}` | 필요 | 장소 상세 조회 |
| `GET` | `/api/v1/places/{placeId}/accessibilities` | 필요 | 장소 접근성 정보 조회 |
| `GET` | `/api/v1/facilities` | 필요 | 편의시설 선택지 조회 |
| `GET` | `/api/v1/tourism-preferences` | 필요 | 관광 취향 선택지 조회 |
| `GET` | `/api/v1/course-keywords` | 필요 | 코스 관심 키워드 선택지 조회 |

### 코스 생성/관리

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/courses` | 필요 | 내 코스 목록 조회 |
| `POST` | `/api/v1/courses` | 필요 | 코스 직접 생성 |
| `GET` | `/api/v1/courses/{courseId}` | 필요 | 코스 상세 조회 |
| `PATCH` | `/api/v1/courses/{courseId}` | 필요 | 코스 기본 정보 수정 |
| `DELETE` | `/api/v1/courses/{courseId}` | 필요 | 코스 삭제 |
| `POST` | `/api/v1/courses/{courseId}/participants` | 필요 | 코스 참여 가족 구성원 추가 |
| `GET` | `/api/v1/courses/{courseId}/participants` | 필요 | 코스 참여자 목록 조회 |
| `DELETE` | `/api/v1/courses/{courseId}/participants/{participantId}` | 필요 | 코스 참여자 삭제 |
| `PUT` | `/api/v1/courses/{courseId}/required-places` | 필요 | 코스 필수 방문 장소 저장 |
| `GET` | `/api/v1/courses/{courseId}/required-places` | 필요 | 코스 필수 방문 장소 조회 |
| `PUT` | `/api/v1/courses/{courseId}/keywords` | 필요 | 코스 관심 키워드 저장 |
| `GET` | `/api/v1/courses/{courseId}/keywords` | 필요 | 코스 관심 키워드 조회 |
| `PUT` | `/api/v1/courses/{courseId}/schedule-items` | 필요 | 일자별 방문 일정 저장 |
| `GET` | `/api/v1/courses/{courseId}/schedule-items` | 필요 | 일자별 방문 일정 조회 |
| `PATCH` | `/api/v1/courses/{courseId}/schedule-items/{scheduleItemId}` | 필요 | 방문 일정 단건 수정 |
| `DELETE` | `/api/v1/courses/{courseId}/schedule-items/{scheduleItemId}` | 필요 | 방문 일정 단건 삭제 |

### AI 추천

AI 엔진은 추후 별도 레포에서 개발하고, 백엔드는 추천 요청/상태/결과 저장과 API 응답을 담당합니다.

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/courses/recommendations` | 필요 | AI 코스 추천 요청 |
| `GET` | `/api/v1/courses/recommendations/{recommendationId}` | 필요 | AI 추천 생성 상태 조회 |
| `POST` | `/api/v1/courses/recommendations/{recommendationId}/confirm` | 필요 | 추천 결과를 코스로 확정 |

### 코스 찜

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/course-likes` | 필요 | 내가 찜한 코스 목록 조회 |
| `POST` | `/api/v1/courses/{courseId}/like` | 필요 | 코스 찜 등록 |
| `DELETE` | `/api/v1/courses/{courseId}/like` | 필요 | 코스 찜 취소 |

### 여행 앨범/사진

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/albums` | 필요 | 내 앨범 목록 조회 |
| `POST` | `/api/v1/albums` | 필요 | 앨범 생성 |
| `GET` | `/api/v1/albums/{albumId}` | 필요 | 앨범 상세 조회 |
| `PATCH` | `/api/v1/albums/{albumId}` | 필요 | 앨범 정보 수정 |
| `DELETE` | `/api/v1/albums/{albumId}` | 필요 | 앨범 삭제 |
| `POST` | `/api/v1/albums/{albumId}/photos` | 필요 | 사진 등록 |
| `GET` | `/api/v1/albums/{albumId}/photos` | 필요 | 앨범 사진 목록 조회 |
| `GET` | `/api/v1/photos/{photoId}` | 필요 | 사진 상세 조회 |
| `PATCH` | `/api/v1/photos/{photoId}` | 필요 | 사진 정보 수정 |
| `DELETE` | `/api/v1/photos/{photoId}` | 필요 | 사진 삭제 |
| `PATCH` | `/api/v1/albums/{albumId}/cover-photo` | 필요 | 앨범 커버 사진 변경 |

### 만족도 평가/리뷰

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/courses/{courseId}/review` | 필요 | 코스 리뷰 조회 |
| `POST` | `/api/v1/courses/{courseId}/review` | 필요 | 코스 만족도 평가 등록 |
| `PATCH` | `/api/v1/reviews/{reviewId}` | 필요 | 리뷰 수정 |
| `DELETE` | `/api/v1/reviews/{reviewId}` | 필요 | 리뷰 삭제. `review`는 `deleted_at`이 없어 hard delete 처리 |

### 마이페이지

마이페이지 하위 API는 도메인 API와 같은 원천 데이터를 사용하되, 마이페이지 화면에 맞는 요약 필드, 정렬, 페이지네이션을 제공하는 UI 전용 조회 API입니다. 별도 테이블을 만들지 않고 각 도메인의 조회 로직을 조합합니다.

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/mypage` | 필요 | 마이페이지 요약 조회 |
| `GET` | `/api/v1/mypage/courses` | 필요 | 내가 만든 코스 목록 조회 |
| `GET` | `/api/v1/mypage/albums` | 필요 | 내 앨범 목록 조회 |
| `GET` | `/api/v1/mypage/reviews` | 필요 | 내가 작성한 리뷰 목록 조회 |
| `GET` | `/api/v1/mypage/liked-courses` | 필요 | 내가 찜한 코스 목록 조회 |

### 알림

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/notifications` | 필요 | 알림 목록 조회 |
| `GET` | `/api/v1/notifications/unread-count` | 필요 | 읽지 않은 알림 수 조회 |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | 필요 | 알림 읽음 처리 |
| `PATCH` | `/api/v1/notifications/read-all` | 필요 | 전체 알림 읽음 처리 |
| `DELETE` | `/api/v1/notifications/{notificationId}` | 필요 | 알림 삭제 |

## 패키지 매핑

| API 영역 | 패키지 |
| --- | --- |
| 회원/인증 | `domain.auth`, `domain.user` |
| 온보딩/가족 구성원 | `domain.onboarding`, `domain.family` |
| 지역/장소/편의시설 | `domain.region`, `domain.place`, `domain.facility` |
| 관광 취향 | `domain.tourism` |
| 코스 관심 키워드 | `domain.course.keyword` |
| 코스 필수 방문 장소 | `domain.course.place` |
| 코스 일정 | `domain.course.schedule` |
| 코스 | `domain.course` |
| AI 추천 | `domain.recommendation`, `infra.ai` |
| 앨범/사진 | `domain.album`, `domain.photo` |
| 리뷰 | `domain.review` |
| 마이페이지 | `domain.mypage` |
| 알림 | `domain.notification` |

## 구현 시 주의사항

- API path는 이 문서를 우선 기준으로 삼고, 화면/Notion 명세와 충돌하면 문서를 먼저 갱신합니다.
- `home`, `mypage`는 여러 도메인을 조합하는 조회 API이므로 별도 테이블을 만들지 않습니다.
- 마이페이지 하위 목록 API는 도메인 API와 같은 데이터를 사용하되, 마이페이지 화면에 맞는 요약 필드와 정렬 기준을 적용합니다.
- `required-places`, `keywords`, `schedule-items`처럼 코스 하위 리소스는 `courseId` 하위 path로 둡니다.
- 리뷰는 현재 ERD에 `deleted_at`이 없으므로 삭제가 필요하면 hard delete로 처리합니다.
- 파일 업로드 방식은 추후 S3 등 저장소 결정 후 `multipart/form-data`로 별도 명시합니다.
