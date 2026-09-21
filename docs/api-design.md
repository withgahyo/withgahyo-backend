# API 설계

이 문서는 백엔드 개발 에이전트가 컨트롤러, 서비스, DTO를 만들 때 기준으로 삼는 API 목록입니다.

최신 기준 문서는 Notion `API 명세서`의 `API 목록` 데이터베이스입니다.

- Notion 문서: `https://app.notion.com/p/3bf69b42aa5b8068a880df3836b5e647`
- 동기화 기준 시각: 2026-08-20
- API 목록 기준: Notion `API 목록` 데이터베이스 53개 행

세부 request/response 필드는 기능 구현 시 Notion 각 API 상세 페이지와 화면 요구사항에 맞춰 보강합니다.

## 기본 규칙

| 항목 | 값 |
| --- | --- |
| Base URL | `https://api.gatigahyo.com` |
| Base Path | `/api/v1` |
| Content-Type | `application/json` |
| 인증 방식 | Bearer JWT |
| 인증 헤더 | `Authorization: Bearer {accessToken}` |

삭제 API는 별도 명시가 없으면 soft delete를 기본으로 합니다.
(예외: `review`는 `deleted_at` 컬럼이 없어 hard delete로 처리합니다.)

## HTTP 상태 코드

| Status | 의미 |
| --- | --- |
| 200 | 조회·수정 성공 |
| 201 | 리소스 생성 성공 |
| 202 | 비동기 작업 요청 접수 |
| 204 | 응답 본문 없는 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 접근 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복·상태 충돌 |
| 500 | 서버 내부 오류 |

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

### AUTH

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-AUTH-001 | `POST` | `/api/v1/auth/login/kakao` | 불필요 | 카카오 소셜 로그인 |
| API-AUTH-002 | `POST` | `/api/v1/auth/login/google` | 불필요 | 구글 소셜 로그인 |
| API-AUTH-003 | `POST` | `/api/v1/auth/logout` | 필요 | 로그아웃 |
| API-AUTH-004 | `POST` | `/api/v1/auth/token/refresh` | 불필요 | 토큰 재발급 |
| API-AUTH-005 | `DELETE` | `/api/v1/users/me` | 필요 | 회원 탈퇴 |

#### 소셜 로그인 요청 정책

카카오/구글 로그인은 프런트엔드가 각 OAuth 제공자에게서 받은 Authorization Code를 백엔드로 전달하고, 백엔드가 제공자 토큰 엔드포인트에서 Access Token을 발급받아 사용자 정보를 조회합니다. 제공자 Access Token을 프런트엔드에서 백엔드로 직접 전달하지 않습니다.

`POST /api/v1/auth/login/kakao`

```json
{
  "authorizationCode": "kakao-authorization-code",
  "redirectUri": "http://localhost:5173/oauth/kakao/callback"
}
```

`POST /api/v1/auth/login/google`

```json
{
  "authorizationCode": "google-authorization-code",
  "redirectUri": "http://localhost:5173/oauth/google/callback"
}
```

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `authorizationCode` | 예 | OAuth 제공자의 인가 코드입니다. |
| `redirectUri` | 예 | 인가 코드 발급 시 사용한 Redirect URI와 동일해야 합니다. |

OAuth 앱 키와 시크릿은 백엔드 환경변수로 관리합니다.

| 환경변수 | 설명 |
| --- | --- |
| `KAKAO_CLIENT_ID` | 카카오 REST API Key |
| `KAKAO_CLIENT_SECRET` | 카카오 Client Secret. 사용하지 않으면 빈 값 허용 |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret |

### ONB

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-ONB-001 | `GET` | `/api/v1/users/me/onboarding` | 필요 | 온보딩 정보 조회 |
| API-ONB-002 | `PUT` | `/api/v1/users/me/onboarding` | 필요 | 온보딩 정보 저장·수정 |
| API-ONB-003 | `GET` | `/api/v1/onboarding/tourism-preferences` | 필요 | 관광 취향 선택지 조회 |
| API-ONB-004 | `GET` | `/api/v1/onboarding/food-preferences` | 필요 | 식사 취향 선택지 조회 |
| API-ONB-006 | `PATCH` | `/api/v1/users/me/onboarding/tourism-preferences` | 필요 | 관광 취향 저장 |
| API-ONB-007 | `PATCH` | `/api/v1/users/me/onboarding/food-preferences` | 필요 | 식사 취향 저장 |
| API-ONB-008 | `PATCH` | `/api/v1/users/me/onboarding/conditions` | 필요 | 여행 컨디션 저장 |
| API-ONB-009 | `POST` | `/api/v1/users/me/onboarding/complete` | 필요 | 온보딩 완료 |

- 여행 기간 Step이 프론트 온보딩 플로우에서 제거되어 `API-ONB-005`(`PATCH /api/v1/users/me/onboarding/trip-duration`)는 삭제했습니다. 번호는 재사용하지 않고 결번으로 둡니다.
- `API-ONB-008`(컨디션 저장)과 `API-ONB-002`(온보딩 정보 저장·수정)의 컨디션 항목(`walkingTolerance`, `restPreference`, `stairsPreference`, `slopePreference`, `spicyPreference`)은 전부 선택 입력입니다.
- `API-ONB-009`(온보딩 완료)의 필수 조건은 관광 취향 1개 이상, 식사 취향 1개 이상입니다. 컨디션 항목은 완료 조건에 포함되지 않습니다.

### FAM

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-FAM-001 | `GET` | `/api/v1/family/members` | 필요 | 가족 구성원 조회, 이메일 기반 가족 연결 후보 조회 |
| API-FAM-003 | `POST` | `/api/v1/family/members` | 필요 | 가족 구성원 연결 |
| API-FAM-004 | `DELETE` | `/api/v1/family/members/{familyMemberId}` | 필요 | 가족 연결 해제 |

- `GET /api/v1/family/members`는 `email` 쿼리 파라미터가 없으면 연결된 가족 구성원 목록을 반환합니다.
- `GET /api/v1/family/members?email={email}`은 이메일 기반 가족 연결 후보를 조회합니다.
- 이메일 기반 후보 조회는 가입된 활성 회원만 반환하며, 이메일은 마스킹해서 응답합니다.
- 이미 연결된 가족은 후보 조회에서 `alreadyConnected: true`로 응답하고, 실제 연결 생성 요청에서는 `409 Conflict`로 처리합니다.
- 같은 이메일에 활성 계정이 여러 개 연결된 경우 후보를 특정할 수 없으므로 `409 Conflict`로 처리합니다.
- 연결 해제는 `family_relation.deleted_at`을 설정하는 soft delete로 처리합니다.
- 탈퇴한 가족 구성원은 가족 구성원 목록과 이미 연결 여부 판단에서 제외합니다.

### HOME

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-HOME-001 | `GET` | `/api/v1/home` | 필요 | 홈 정보 조회 |
| API-HOME-002 | `GET` | `/api/v1/courses/popular` | 필요 | 인기 코스 조회 |

### WEATHER

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-WEATHER-001 | `GET` | `/api/v1/weather/upcoming` | 필요 | 가장 가까운 예정 여행의 단기예보 조회 |

- 현재 로그인 사용자가 creator 또는 participant인 `UPCOMING` 코스 중 `startDate >= today`인 가장 가까운 코스 1개만 대상으로 합니다.
- 대표 위치는 해당 코스 확정 일정의 Day 1, visitOrder가 가장 빠른 `CourseScheduleItem`의 `Place` 좌표를 기상청 격자(nx/ny)로 변환해 사용합니다(Region에는 좌표가 없어 사용하지 않음).
- 기상청 단기예보(getVilageFcst)만 사용하며, 예보 범위 밖 날짜는 임의로 만들어내지 않고 `dailyForecasts`에서 제외합니다. 여행 전체/일부가 예보 범위인지, 예정 여행이 없는지, 위치를 못 찾았는지, 외부 API 오류인지는 `status`로 구분합니다.
- 날씨 데이터는 DB에 저장하지 않고 매 요청마다 조회합니다.

### CRS

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-CRS-001 | `GET` | `/api/v1/regions/search` | 필요 | 여행 지역 검색 |
| API-CRS-002 | `GET` | `/api/v1/places/search` | 필요 | 꼭 가고 싶은 장소 검색 |
| API-CRS-003 | `GET` | `/api/v1/course-keywords/suggestions` | 필요 | 관심 키워드 추천 |
| API-CRS-004 | `GET` | `/api/v1/users/me/family-members` | 필요 | 코스 생성용 가족 선택지 조회 |
| API-CRS-005 | `POST` | `/api/v1/courses` | 필요 | 코스 초안 생성 |

#### CRS 지역·장소 검색 정책

`GET /api/v1/regions/search`

여행 지역 선택용 API입니다. 지역은 전국을 대상으로 하며, 관광공사 지역 코드 체계인 `areaCode`, `sigunguCode`를 기준으로 반환합니다.

Query Parameters:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `query` | 아니오 | 지역명 검색어입니다. 없으면 전체 지역을 반환할 수 있습니다. |

Response:

```json
{
  "regions": [
    {
      "areaCode": "3",
      "sigunguCode": "1",
      "name": "대전광역시 동구",
      "parentName": "대전광역시",
      "displayName": "대전 동구"
    }
  ]
}
```

`GET /api/v1/places/search`

꼭 가고 싶은 장소 검색용 API입니다. 사용자가 먼저 선택한 여행 지역 안에서만 장소를 검색할 수 있습니다.

Query Parameters:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `areaCode` | 예 | 선택한 여행 지역의 관광공사 시도 코드입니다. |
| `sigunguCode` | 아니오 | 선택한 여행 지역의 관광공사 시군구 코드입니다. 시군구까지 선택한 경우 전달합니다. |
| `query` | 예 | 장소 검색어입니다. |
| `cursor` | 아니오 | 다음 검색 결과를 이어서 조회할 때 사용하는 커서입니다. 첫 요청에서는 전달하지 않습니다. |
| `size` | 아니오 | 한 번에 조회할 장소 수입니다. 기본값은 구현 시 정합니다. |

장소 검색은 한국관광공사 `한국관광공사_국문 관광정보 서비스_GW`의 `searchKeyword2`와 카카오 Local API의 `키워드로 장소 검색`을 함께 활용합니다. 지역 코드는 한국관광공사 `areaCode2` 기준을 사용합니다. 카카오 Local API 결과는 주소·좌표 보강과 관광공사 검색 결과 보완에 사용하며, 필요하면 `주소로 좌표 변환`, `좌표로 행정구역정보 받기` API로 지역 일치 여부를 확인합니다. 백엔드는 외부 API 응답을 그대로 노출하지 않고 서비스의 `place` 리소스 형태로 정규화합니다. 검색 결과는 요청한 `areaCode`와 `sigunguCode` 범위에 속한 장소만 반환합니다.

Response:

```json
{
  "places": [
    {
      "placeId": 501,
      "source": "TOUR_API",
      "externalPlaceId": "126508",
      "name": "한밭수목원",
      "category": "NATURE",
      "address": "대전광역시 서구 둔산대로 169",
      "areaCode": "3",
      "sigunguCode": "1",
      "imageUrl": "https://example.com/place.jpg",
      "latitude": 36.366,
      "longitude": 127.388
    }
  ],
  "hasNext": false,
  "nextCursor": null
}
```

`source`는 장소 데이터의 기준 출처이며 `TOUR_API`, `KAKAO` 등을 사용할 수 있습니다. `externalPlaceId`는 외부 API의 장소 식별자입니다.

`hasNext`가 `true`이면 다음 검색 결과가 남아 있다는 뜻입니다. 이때 `nextCursor` 값을 다음 요청의 `cursor`로 전달해 이어서 조회합니다. 마지막 결과이면 `hasNext`는 `false`, `nextCursor`는 `null`입니다.

`POST /api/v1/courses`에서 `mustVisitPlaceIds`를 저장할 때는 코스의 `areaCode`, `sigunguCode`와 각 장소의 지역이 일치하는지 다시 검증합니다. 지역이 다른 장소는 코스의 꼭 가고 싶은 장소로 저장하지 않습니다.

### REC

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-REC-001 | `POST` | `/api/v1/courses/{courseId}/generations` | 필요 | AI 추천 생성 시작 |
| API-REC-002 | `GET` | `/api/v1/course-generations/{generationId}` | 필요 | AI 추천 생성 상태 조회 |
| API-REC-003 | `GET` | `/api/v1/course-generations/{generationId}/candidates` | 필요 | AI 추천 후보 목록 조회 |
| API-REC-004 | `GET` | `/api/v1/course-generations/{generationId}/candidates/{candidateId}` | 필요 | AI 추천 후보 상세 조회 |
| API-REC-005 | `POST` | `/api/v1/course-generations/{generationId}/selection` | 필요 | AI 추천 후보 확정 |

### INTERNAL

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-INT-001 | `POST` | `/api/v1/internal/places/upsert` | 내부 키 | AI 서버 장소 upsert |

#### AI 서버 장소 upsert 정책

`POST /api/v1/internal/places/upsert`

AI 서버가 한국관광공사, 카카오 등 외부 API로 조회한 장소를 백엔드 `place` 리소스로 저장하거나 갱신할 때 사용하는 내부 API입니다. 이 API는 프런트엔드 사용자 JWT가 아니라 서버 간 통신용 `X-Internal-Api-Key` 헤더로 보호합니다. 실제 키는 `INTERNAL_AI_API_KEY` 환경변수로 관리합니다.

Request Headers:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `X-Internal-Api-Key` | 예 | AI 서버와 백엔드가 공유하는 내부 API 키입니다. |

Request:

```json
{
  "places": [
    {
      "source": "TOUR_API",
      "contentId": "126508",
      "contentTypeId": "12",
      "category": "NATURE",
      "areaCode": "3",
      "sigunguCode": "1",
      "name": "한밭수목원",
      "address": "대전광역시 서구 둔산대로 169",
      "latitude": 36.366,
      "longitude": 127.388,
      "imageUrl": "https://example.com/place.jpg"
    }
  ]
}
```

Response:

```json
{
  "places": [
    {
      "placeId": 501,
      "source": "TOUR_API",
      "contentId": "126508",
      "contentTypeId": "12",
      "name": "한밭수목원"
    }
  ]
}
```

기존 장소는 `contentId`, `contentTypeId` 기준으로 찾아 외부 정보를 갱신하고, 없는 장소는 새로 저장합니다. `areaCode`, `sigunguCode`에 해당하는 지역이 없으면 `PLACE_404_001`로 실패합니다.

### EXP

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-EXP-001 | `GET` | `/api/v1/courses` | 필요 | 코스 검색 |
| API-EXP-003 | `GET` | `/api/v1/courses/similar-family` | 필요 | 유사 가족 코스 조회 |
| API-EXP-004 | `GET` | `/api/v1/courses/{courseId}` | 필요 | 코스 상세 조회 |
| API-EXP-005 | `POST` | `/api/v1/courses/{courseId}/likes` | 필요 | 코스 찜 등록 |
| API-EXP-006 | `DELETE` | `/api/v1/courses/{courseId}/likes` | 필요 | 코스 찜 해제 |
| API-EXP-007 | `POST` | `/api/v1/courses/{courseId}/confirm` | 필요 | 코스 확정 |
| API-EXP-008 | `PATCH` | `/api/v1/courses/{courseId}` | 필요 | 코스 기본 정보 수정 |
| API-EXP-009 | `DELETE` | `/api/v1/courses/{courseId}` | 필요 | 코스 삭제 |

### ALB

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-ALB-001 | `GET` | `/api/v1/albums` | 필요 | 공동 앨범 목록 조회 |
| API-ALB-002 | `GET` | `/api/v1/albums/{albumId}` | 필요 | 공동 앨범 상세 조회 |
| API-ALB-003 | `POST` | `/api/v1/albums/{albumId}/photos/presigned-urls` | 필요 | 사진 업로드 Presigned URL 발급 |
| API-ALB-004 | `POST` | `/api/v1/albums/{albumId}/photos/complete` | 필요 | 사진 업로드 완료 등록 |
| API-ALB-005 | `POST` | `/api/v1/albums/{albumId}/videos` | 필요 | 여행 영상 생성 요청 |
| API-ALB-006 | `GET` | `/api/v1/albums/{albumId}/videos/{jobId}` | 필요 | 여행 영상 생성 상태 조회 |
| API-ALB-007 | `DELETE` | `/api/v1/albums/{albumId}/photos/{photoId}` | 필요 | 앨범 사진 삭제 |
| API-ALB-008 | `PATCH` | `/api/v1/albums/{albumId}` | 필요 | 여행 앨범 정보 수정 |
| API-ALB-009 | `PATCH` | `/api/v1/albums/{albumId}/photos/{photoId}` | 필요 | 앨범 사진 정보 수정 |

### REV

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-REV-001 | `GET` | `/api/v1/users/me/reviews/pending` | 필요 | 만족도 평가 대상 조회 |
| API-REV-002 | `POST` | `/api/v1/courses/{courseId}/reviews` | 필요 | 여행 만족도 등록 |
| API-REV-003 | `GET` | `/api/v1/courses/{courseId}/review-form` | 필요 | 만족도 평가 폼 조회 |
| API-REV-004 | `GET` | `/api/v1/courses/{courseId}/reviews/me` | 필요 | 내 만족도 평가 조회 |
| API-REV-005 | `PATCH` | `/api/v1/courses/{courseId}/reviews/me` | 필요 | 만족도 평가 수정 |

### COM

커뮤니티는 사용자가 작성한 여행 후기(`Review`)를 공유하고, 별점·좋았던점·후기 글을 다른 사용자와 함께 보는 공간입니다. `CommunityPost`는 항상 하나의 `Review`를 1:1로 참조하며, 자체적인 제목/본문을 갖지 않습니다.

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-COM-001 | `POST` | `/api/v1/community/posts` | 필요 | 내 여행 후기를 커뮤니티에 공유 |
| API-COM-002 | `DELETE` | `/api/v1/community/posts/{postId}` | 필요 | 공유한 게시글 삭제(공유 취소) |
| API-COM-003 | `GET` | `/api/v1/community/posts` | 필요 | 커뮤니티 게시글 목록·검색·필터 조회 |
| API-COM-004 | `GET` | `/api/v1/community/posts/recommendations` | 필요 | 좋아요가 많은 추천 후기 Top N 조회 |
| API-COM-005 | `GET` | `/api/v1/community/posts/{postId}` | 필요 | 커뮤니티 게시글 상세 조회 |
| API-COM-006 | `GET` | `/api/v1/community/posts/{postId}/share-url` | 필요 | 커뮤니티 게시글 공유 URL 조회 |
| API-COM-007 | `POST` | `/api/v1/community/posts/{postId}/likes` | 필요 | 게시글 좋아요 |
| API-COM-008 | `DELETE` | `/api/v1/community/posts/{postId}/likes` | 필요 | 게시글 좋아요 취소 |
| API-COM-009 | `GET` | `/api/v1/community/posts/{postId}/comments` | 필요 | 커뮤니티 댓글 목록 조회 |
| API-COM-010 | `POST` | `/api/v1/community/posts/{postId}/comments` | 필요 | 커뮤니티 댓글 작성 |
| API-COM-011 | `POST` | `/api/v1/community/posts/{postId}/reports` | 필요 | 커뮤니티 게시글 신고 |
| API-COM-012 | `POST` | `/api/v1/community/users/{userId}/blocks` | 필요 | 커뮤니티 게시글 작성자 차단 |

- 게시글 공유는 본인이 작성한 `Review`만 가능하며, 이미 공유한 후기를 다시 공유하면 409(`POST_ALREADY_SHARED`)를 반환합니다.
- 게시글 목록/상세 응답은 작성자 정보(`authorId`, `authorNickname`, `authorProfileImageUrl`), 코스 정보(`courseId`, `courseTitle`, `regionName`, `courseImageUrl`), 별점(`rating`), 좋았던점(`highlights`), 후기 글(`content`/`contentPreview`), 좋아요 수(`likeCount`)와 내가 좋아요를 눌렀는지 여부(`likedByMe`)를 포함합니다.
- 게시글 목록은 `keyword`, `highlightType`, `sort`(`latest`|`popular`), `cursor`, `size` 쿼리 파라미터를 지원합니다. `highlightType`은 후기에 등록된 좋았던점 태그로 필터링합니다.
- 게시글 목록과 댓글 목록은 커서 기반 페이지네이션을 사용하며, `hasNext`와 `nextCursor`를 응답합니다.
- 좋아요는 사용자당 게시글별로 1회만 반영되도록 토글로 동작하며(중복 요청은 무시), 좋아요/좋아요 취소 응답에 최신 `likeCount`를 함께 반환합니다.
- 댓글 작성, 게시글 신고, 작성자 차단은 인증 사용자 기준으로 처리합니다.
- 작성자 차단은 자기 자신을 차단할 수 없습니다.
- 게시글 삭제는 본인이 공유한 게시글만 가능하며 soft delete로 처리합니다.

### MY

| API ID | Method | Path | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| API-MY-002 | `PATCH` | `/api/v1/users/me` | 필요 | 내 프로필 수정 |
| API-MY-003 | `POST` | `/api/v1/users/me/profile-image` | 필요 | 내 프로필 이미지 업로드 |

`PATCH /api/v1/users/me`

Request:

```json
{
  "nickname": "지우",
  "profileImageUrl": "https://example.com/profile.png"
}
```

Response:

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "지우",
  "profileImageUrl": "https://example.com/profile.png"
}
```

- `nickname`, `profileImageUrl` 중 하나 이상을 전달해야 합니다.
- `nickname`은 앞뒤 공백을 제거한 뒤 2자 이상 20자 이하로 저장합니다.
- `profileImageUrl`은 `null`이면 기존 값을 유지하고, 문자열 값이 전달되면 그대로 저장합니다.

`POST /api/v1/users/me/profile-image`

Request:

```http
Content-Type: multipart/form-data
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `file` | file | Y | 업로드할 프로필 이미지. `jpg`, `png`, `webp` 형식을 지원합니다. |

Response:

```json
{
  "profileImageUrl": "/uploads/profile-images/1-uuid.png"
}
```

- 업로드 성공 시 서버 로컬 `app.upload.profile-images-dir`에 파일을 저장하고, `users.profile_image_url`을 응답 URL로 갱신합니다.
- 기본 저장 경로는 `uploads/profile-images`, 기본 공개 URL prefix는 `/uploads/profile-images`입니다.
- 저장된 이미지는 `/uploads/profile-images/**` 정적 리소스로 조회할 수 있습니다.

## 패키지 매핑

| API 영역 | 패키지 |
| --- | --- |
| AUTH | `domain.auth`, `domain.user` |
| ONB | `domain.onboarding`, `domain.user`, `domain.family` |
| FAM | `domain.family`, `domain.user` |
| HOME | `domain.home` |
| WEATHER | `domain.weather`, `infra.weather` |
| CRS | `domain.course`, `domain.place`, `domain.family` |
| REC | `domain.recommendation`, `infra.ai` |
| INTERNAL | `domain.place` |
| EXP | `domain.course` |
| ALB | `domain.album`, `domain.video` |
| REV | `domain.review` |
| COM | `domain.community` |
| MY | `domain.user` |

## 구현 시 주의사항

- API path는 이 문서를 우선 기준으로 삼고, 화면/Notion 명세와 충돌하면 Notion 최신 `API 목록` 데이터베이스를 확인한 뒤 문서를 갱신합니다.
- 각 API의 request/response, validation, error, 세부 정책은 Notion의 API 상세 페이지를 기준으로 구현합니다.
- 가입 사용자 기반 가족 연결 정책과 충돌하는 가족 프로필 직접 생성·수정 방식은 사용하지 않습니다.
- 사진 업로드는 Presigned URL 발급과 업로드 완료 등록 흐름을 기준으로 구현합니다.
- 신규 여행 영상 생성 API는 앨범 하위 리소스로 구현합니다.
- `home`은 여러 도메인을 조합하는 조회 API이므로 별도 테이블을 만들지 않습니다.
- 리뷰는 현재 ERD에 `deleted_at`이 없으므로 삭제가 필요하면 hard delete로 처리합니다.
