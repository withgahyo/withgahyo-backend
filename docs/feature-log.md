# Feature Log

기능 구현 시 이슈, 작업 내용, 구현 이유와 방식을 기록합니다.

## 작성 기준

- 기능 단위로 작성합니다.
- 단순 오타, 포맷팅, import 정리는 기록하지 않습니다.
- API, ERD, 공통 응답, 예외 처리, 인증 흐름에 영향이 있으면 기록합니다.
- 새 기록은 아래 `기록` 표의 마지막 줄에 추가합니다.

## 템플릿

| 날짜 | 이슈 | 작업 내용 | 왜/어떻게 구현했는지 |
| --- | --- | --- | --- |
| 2026-08-20 | feat/1-auth-api | 소셜 로그인 Authorization Code 방식 전환 | 프런트엔드가 제공자 Access Token을 백엔드로 직접 전달하지 않도록 로그인 요청을 `authorizationCode`, `redirectUri` 기반으로 변경했습니다. 백엔드가 카카오/구글 토큰 엔드포인트에서 Access Token을 발급받은 뒤 사용자 정보를 조회하며, OAuth Client ID와 Secret은 서버 환경변수로 관리합니다. |
| 2026-08-19 | feat/1-auth-api | 카카오 소셜 로그인 기반 구현 | Notion AUTH 명세에 맞춰 카카오 OAuth 사용자 정보로 신규 가입 또는 탈퇴 계정 복구를 수행하고, JWT Access Token과 서버 저장 Refresh Token을 발급하도록 `domain.auth` 기반 구조를 추가했습니다. Refresh Token Rotation 요구를 지원하기 위해 `refresh_token` 엔티티와 ERD 기록을 추가했습니다. |
| 2026-08-19 | feat/1-auth-api | 구글 소셜 로그인 구현 | 구글 OAuth 사용자 정보의 `sub`, `name`, `picture`를 서비스 사용자 식별과 프로필 갱신에 사용하고, 카카오 로그인과 같은 JWT·Refresh Token 발급 흐름을 재사용하도록 구현했습니다. |
| 2026-08-19 | feat/1-auth-api | 로그아웃 구현 | JWT Access Token으로 인증된 사용자 ID를 SecurityContext에 저장하고, 로그아웃 시 해당 사용자의 활성 Refresh Token 세션을 모두 폐기하도록 구현했습니다. |
| 2026-08-19 | feat/1-auth-api | 토큰 재발급 구현 | Refresh Token JWT와 서버 저장 세션을 함께 검증하고, 정상 재발급 시 기존 Refresh Token을 폐기한 뒤 새 Access Token과 Refresh Token을 발급하도록 Rotation을 구현했습니다. |
| 2026-08-19 | feat/1-auth-api | 회원 탈퇴 구현 | 확정된 재가입 정책에 따라 회원 탈퇴 시 `users.deleted_at`을 설정하고 해당 사용자의 Refresh Token 세션을 모두 폐기합니다. 가족 관계 row는 삭제하지 않고, 같은 소셜 계정 재로그인 시 기존 row를 복구하는 로그인 흐름과 연계했습니다. |
| YYYY-MM-DD | #이슈번호 / 이슈명 | 무엇을 구현했는지 | 왜 이 방식으로 구현했고, 어떤 흐름으로 구현했는지 |

## 기록

| 날짜 | 이슈 | 작업 내용 | 왜/어떻게 구현했는지 |
| --- | --- | --- | --- |
| 2026-08-23 | #7 / CRS 코스 생성 API 및 지역 기반 장소 검색 구현 | CRS 5개 API 구현 | 코스 생성 플로우에서 지역 검색, 지역 기반 장소 검색, 관심 키워드 추천, 가족 선택지 조회, 코스 초안 생성을 사용할 수 있도록 `domain.course`, `domain.place`, `domain.family` 기반 컨트롤러·서비스·Repository를 추가했습니다. 장소 검색은 한국관광공사 `searchKeyword2`와 카카오 Local API를 외부 포트로 분리해 정규화하고, 코스 초안 생성 시 가족 연결 여부와 장소 지역 일치 여부를 다시 검증하도록 구현했습니다. |
| 2026-08-28 | #2 / 온보딩 플로우 구현 | 온보딩 API 필드/완료 조건 수정 | 프론트 온보딩 UI 최종 확정에 맞춰 여행 기간(`tripDuration`) Step을 삭제하고 `API-ONB-005`(`PATCH .../trip-duration`) 엔드포인트·`UserOnboardingProfile.tripDuration` 필드·관련 DTO/Service 로직을 제거했습니다. 컨디션 5개 필드(`walkingTolerance`, `restPreference`, `stairsPreference`, `slopePreference`, `spicyPreference`)는 프론트가 전부 필수로 강제하지 않기로 해 `@NotBlank`를 제거하고 `@Size(max=30)`만 유지하는 선택 입력으로 바꿨습니다. `POST .../complete`의 완료 조건도 관광 취향 1개 이상 + 식사 취향 1개 이상으로만 판단하도록 단순화하고, 프로필 row가 없어도 두 취향만 있으면 새로 생성한 뒤 완료 처리하도록 했습니다. 이 과정에서 그사이 도입된 JWT 인증(`AuthenticatedUser`)에 맞춰 `OnboardingController`의 임시 사용자 식별 로직도 `CourseController`/`FamilyController`와 동일한 `@AuthenticationPrincipal AuthenticatedUser` + `requireUserId` 패턴으로 교체했습니다. |
