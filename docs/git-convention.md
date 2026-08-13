# Git Convention

## 브랜치 전략

```text
main
└── develop
     └── feature 브랜치
```

- **main**: 배포 가능한 안정 버전
- **develop**: 기본 개발 브랜치
- **feature 브랜치**: 기능 단위 개발

## 이슈명 규칙

```text
[Feat] 기능명 구현
```

예시:

```text
[Feat] Splash/Login 화면 구현
[Feat] 온보딩 플로우 구현
[Feat] 홈 화면 구현
[Feat] 코스 생성 화면 구현
```

## 브랜치명 규칙

```text
feat/{이슈번호}-{기능명}
```

예시:

```text
feat/1-splash-login
feat/2-onboarding
feat/3-home
feat/4-course-create
```

## 커밋 메시지 규칙

```text
feat: 기능명 구현
```

예시:

```text
feat: Splash/Login 화면 구현
feat: 온보딩 플로우 구현
feat: 홈 화면 구현
```

### Prefix

| Prefix | 설명 |
| --- | --- |
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 |
| `style` | 코드 스타일 변경 |
| `docs` | 문서 수정 |
| `chore` | 설정 및 빌드 |

## 개발 프로세스

```text
Issue 생성
        ↓
feature 브랜치 생성
        ↓
기능 개발
        ↓
Commit
        ↓
Push
```

## Rule

- 모든 작업은 **Issue 생성 후 진행**합니다.
- 하나의 기능은 **하나의 feature 브랜치**에서 개발합니다.
- `main` 브랜치는 항상 배포 가능한 상태를 유지합니다.
- API path, ERD, 공통 응답 규칙을 변경했다면 관련 문서도 함께 수정합니다.
