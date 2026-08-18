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
| 2026-08-19 | feat/1-auth-api | 카카오 소셜 로그인 기반 구현 | Notion AUTH 명세에 맞춰 카카오 OAuth 사용자 정보로 신규 가입 또는 탈퇴 계정 복구를 수행하고, JWT Access Token과 서버 저장 Refresh Token을 발급하도록 `domain.auth` 기반 구조를 추가했습니다. Refresh Token Rotation 요구를 지원하기 위해 `refresh_token` 엔티티와 ERD 기록을 추가했습니다. |
| 2026-08-19 | feat/1-auth-api | 구글 소셜 로그인 구현 | 구글 OAuth 사용자 정보의 `sub`, `name`, `picture`를 서비스 사용자 식별과 프로필 갱신에 사용하고, 카카오 로그인과 같은 JWT·Refresh Token 발급 흐름을 재사용하도록 구현했습니다. |
| 2026-08-19 | feat/1-auth-api | 로그아웃 구현 | JWT Access Token으로 인증된 사용자 ID를 SecurityContext에 저장하고, 로그아웃 시 해당 사용자의 활성 Refresh Token 세션을 모두 폐기하도록 구현했습니다. |
| 2026-08-19 | feat/1-auth-api | 토큰 재발급 구현 | Refresh Token JWT와 서버 저장 세션을 함께 검증하고, 정상 재발급 시 기존 Refresh Token을 폐기한 뒤 새 Access Token과 Refresh Token을 발급하도록 Rotation을 구현했습니다. |
| 2026-08-19 | feat/1-auth-api | 회원 탈퇴 구현 | 확정된 재가입 정책에 따라 회원 탈퇴 시 `users.deleted_at`을 설정하고 해당 사용자의 Refresh Token 세션을 모두 폐기합니다. 가족 관계 row는 삭제하지 않고, 같은 소셜 계정 재로그인 시 기존 row를 복구하는 로그인 흐름과 연계했습니다. |
| YYYY-MM-DD | #이슈번호 / 이슈명 | 무엇을 구현했는지 | 왜 이 방식으로 구현했고, 어떤 흐름으로 구현했는지 |

## 기록

| 날짜 | 이슈 | 작업 내용 | 왜/어떻게 구현했는지 |
| --- | --- | --- | --- |
