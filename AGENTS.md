# Agent Guide

이 파일은 에이전트가 같이가효 백엔드에서 작업할 때 먼저 확인해야 하는 기준입니다.

## 필수 참조 문서

- API 설계: `docs/api-design.md`
- ERD 설계: `docs/erd-design.md`
- 코드 규칙: `docs/code-convention.md`
- Git 규칙: `docs/git-convention.md`
- 기능 구현 기록: `docs/feature-log.md`
- 충돌 해결 기록: `docs/conflict-log.md`

## 작업 원칙

- API path와 HTTP method는 `docs/api-design.md`를 기준으로 합니다.
- 엔티티, 테이블, 관계, 제약 조건은 `docs/erd-design.md`를 기준으로 합니다.
- 브랜치, 이슈, 커밋 규칙은 `docs/git-convention.md`를 기준으로 합니다.
- 문서와 구현이 충돌하면 임의로 구현하지 않고 문서를 먼저 확인하거나 갱신합니다.
- `.env`, 실제 DB 비밀번호, 토큰, API key 등 민감정보는 커밋하지 않습니다.

## 기능 구현 기록

기능을 구현했거나 API, ERD, 공통 응답, 예외 처리, 인증 흐름에 영향을 주는 변경을 했다면 `docs/feature-log.md`에 기록합니다.

기록은 `기록` 표의 마지막 줄에 추가합니다.

기록 기준:

- 이슈
- 작업 내용
- 왜/어떻게 구현했는지

단순 오타, 포맷팅, import 정리는 기록하지 않습니다.

## 충돌 해결 기록

develop 병합 과정에서 충돌을 해결했다면 `docs/conflict-log.md`에 기록합니다.

기록은 `기록` 표의 마지막 줄에 추가합니다.

기록 기준:

- 충돌 내용
- 해결 방식
- 선택 이유

자동 병합된 내용은 기록하지 않습니다. 공통 설정, 응답, 예외, 엔티티, API path 충돌은 반드시 기록합니다.
