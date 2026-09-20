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
| 2026-08-26 | #11 / EXP 코스 상세·찜·확정·수정·삭제 API 구현 | EXP 6개 API 구현 | 코스 상세 조회, 찜 등록/해제, 확정, 기본 정보 수정, soft delete를 `domain.course` 기반으로 구현했습니다. 현재 ERD에 공개 범위 컬럼이 없어 상세·확정·수정·삭제 권한은 생성자 또는 참여자로 제한하고, 찜 등록/해제는 반복 요청을 멱등 처리하도록 구현했습니다. |
| 2026-09-01 | #13 / REC AI 추천 생성 및 후보 확정 API 구현 | REC 5개 API 구현 | AI 추천 생성 시작, 상태 조회, 후보 목록·상세 조회, 후보 확정 API를 `domain.recommendation` 기반으로 구현했습니다. `RecommendationJobStatus`는 기존 `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`를 유지하고, 후보 확정 시 추천 후보 일정을 실제 코스 일정으로 복사한 뒤 코스를 확정하고 앨범을 생성하도록 처리했습니다. |
| 2026-09-02 | #15 / AI 서버 추천 생성 API 연동 | REC 추천 생성 AI 서버 연동 | 추천 생성 시작 시 `infra.ai`의 AI 서버 client를 통해 `/internal/v1/recommendations/generate`를 호출하고, 응답받은 추천 후보와 일정 아이템을 `RecommendationCandidate`, `RecommendationCandidateItem`으로 저장하도록 구현했습니다. AI 서버 요청에는 코스 지역·일정, 관심 키워드, 필수 방문 장소, 참가자별 온보딩 조건·관광/음식/편의시설 선호 코드를 포함하며, AI 호출 또는 응답 장소 매핑에 실패하면 추천 job을 `FAILED` 상태로 남깁니다. |
| 2026-09-02 | #17 / AI 서버 장소 upsert 내부 API 추가 | INTERNAL 장소 upsert API 구현 | AI 서버가 외부 API로 조회한 장소를 추천 생성 전에 백엔드 `place` 리소스로 저장하거나 갱신할 수 있도록 `POST /api/v1/internal/places/upsert`를 추가했습니다. 기존 `PlaceUpsertWriter`를 재사용해 중복 저장을 막고, 서버 간 통신은 `X-Internal-Api-Key`와 `INTERNAL_AI_API_KEY`로 검증하도록 구현했습니다. |
| 2026-09-12 | #22 / 내 프로필 수정 API 개발 | MY 프로필 수정 API 구현 | 개인정보 관리 화면에서 닉네임과 프로필 이미지 URL을 수정할 수 있도록 `PATCH /api/v1/users/me`를 추가했습니다. 마이페이지 통계 화면은 현재 요구사항에서 제외되어 별도 `mypage` 조합 조회 API는 만들지 않고, 기존 로그인 사용자 정보와 프로필 수정 응답을 프론트에서 갱신하는 흐름으로 정리했습니다. |
| 2026-09-12 | #22 / 프로필 이미지 업로드 API 개발 | MY 프로필 이미지 업로드 API 구현 | 개인정보 관리 화면에서 파일 선택 방식으로 프로필 이미지를 변경할 수 있도록 `POST /api/v1/users/me/profile-image`를 추가했습니다. `multipart/form-data`의 `file`을 검증한 뒤 서버 로컬 업로드 디렉터리에 저장하고, `/uploads/profile-images/**` 정적 리소스 URL을 `users.profile_image_url`에 반영합니다. |
| 2026-09-13 | #24 / 커뮤니티 게시판 API 구현 | COM 8개 API 구현 | 커뮤니티 게시글 목록·추천·상세 조회, 댓글 목록·작성, 게시글 신고, 작성자 차단, 공유 URL 조회를 `domain.community` 기반으로 구현했습니다. 게시글과 댓글은 soft delete 기준으로 조회하고, 목록형 API는 커서 기반 페이지네이션을 사용하며, 신고·차단·댓글 작성은 인증 사용자 기준으로 저장하도록 구성했습니다. |
| 2026-09-15 | #24 / 커뮤니티-후기 연동 및 좋아요 구현 | CommunityPost를 Review 공유 기반으로 재구성 | 자체 `category`/`title`/`content`를 갖던 `CommunityPost`를 제거하고 `review_id`(unique FK)로 `Review`를 1:1 참조하도록 바꿔, "내 여행 후기(별점·좋았던점·후기 글)를 커뮤니티에 공유"하는 흐름으로 재구성했습니다. 목록/상세/추천 응답에 작성자 프로필, 코스명·지역·대표 이미지, 별점, 좋았던점 태그를 채워 추천 탭이 비어 보이던 문제를 해결했습니다. 그동안 API 없이 필드만 있던 좋아요는 `community_post_like`(user_id+post_id 복합키, `course_like`와 동일한 insert-ignore 패턴)를 추가해 `POST/DELETE .../likes` 토글로 구현하고, 응답에 `likedByMe`를 포함했습니다. 게시글 생성(`POST /posts`, 본인 후기만 공유 가능·중복 공유 시 409)과 삭제(`DELETE /posts/{postId}`, 작성자 본인만) API도 추가했고, 목록 필터의 `category` 파라미터는 좋았던점 태그로 필터링하는 `highlightType`으로 교체했습니다. |
| 2026-09-19 | #? / HOME API 구현 및 연동 (branch: feat/home-api, 이슈 번호 확인 필요) | HOME 홈 정보 조회 API 구현 | `docs/api-design.md`의 `HOME → domain.home` 패키지 매핑에 맞춰 `domain.home`(Controller/Service/DTO)을 신설하고 `GET /api/v1/home`을 추가했습니다. 로그인 사용자가 생성했거나 가족 참여자로 속한 `UPCOMING`(soft-delete 제외) 코스를 `startDate` 오름차순으로 반환하며, creator/participant 조건은 EXISTS 서브쿼리로 묶어 중복 반환을 방지했습니다. 대표 이미지는 `Course.imageUrl`(실제 생성 흐름에서 세팅되지 않아 항상 null)을 쓰지 않고, 확정된 `CourseScheduleItem`을 dayNumber·visitOrder 순으로 훑어 첫 유효 `Place.imageUrl`을 사용하도록 해 추천 후보 목록의 `thumbnailImageUrl` 계산과 정책을 통일했습니다. `daysUntilTrip` 계산은 `CourseService`에 있던 로직을 `Course.daysUntilTrip()`으로 옮겨 Course Detail과 Home이 같은 계산을 재사용하게 했습니다. 태그/일정+장소 조회는 courseIds 배치 조회로 처리해 Course 개수와 무관하게 쿼리 수가 고정되도록 구현했습니다. 기획 결정에 따라 찜하기(favoriteCourses) 관련 기능은 포함하지 않았습니다. |
| 2026-09-19 | #? / HOME API 구현 및 연동 (branch: feat/home-api) | HOME 응답에 alternativeCandidates(AI가 함께 추천한 코스) 추가 | 각 familyCourse 아래에 "확정 당시 함께 생성됐지만 선택되지 않은 RecommendationCandidate"를 `alternativeCandidates`로 내려주도록 확장했습니다. `RecommendationJob`/`Course`에 selectedCandidateId 컬럼이 없어, `RecommendationCandidate.selected=true`인 후보가 속한 job을 그 Course의 확정 job으로 판별했습니다(Course.confirm()이 평생 1회만 확정을 허용해 Course당 selected 후보는 최대 1개인 점을 근거로 함 — 재시도로 여러 job이 있어도 모호하지 않음). 그 job의 나머지 후보(선택되지 않은 것)만 rank 순서 그대로 alternativeCandidates로 반환하고, 선택된 job과 무관한 다른 job(버려진 재시도)의 후보는 제외했습니다. `RecommendationCandidateRepository`에 courseIds 기준 배치 조회를 추가해 Course 개수와 무관하게 쿼리 수가 고정되도록 했고, 후보 대표 이미지 계산 로직은 기존 후보 목록 API와 중복 구현하지 않도록 `RecommendationCandidateThumbnails` 유틸리티로 추출해 양쪽에서 공유합니다. DB schema/entity 변경은 없습니다. |
| 2026-09-20 | 배포 준비 / 프로필 이미지 저장소 R2 전환 | 프로필 이미지 업로드를 로컬 디스크에서 Cloudflare R2로 전환 | Render는 배포·재시작마다 디스크가 초기화되어 기존 `uploads/profile-images` 로컬 저장 방식으로는 배포 시 이미지가 유실되는 문제가 있어, AWS SDK v2 S3 클라이언트(`R2Config`)로 R2에 업로드하도록 `ProfileImageStorageService`를 교체했습니다. 업로드 후 URL은 `R2_PUBLIC_BASE_URL` 기준으로 생성하고, 더 이상 필요 없는 로컬 정적 리소스 핸들러(`UploadResourceConfig`)와 `/uploads/profile-images/**` permitAll 설정은 제거했습니다. |
| 2026-09-20 | 배포 준비 / Render 운영 설정 | Docker 빌드와 운영 DB 기준 데이터 초기화 설정 | `prod` 프로필에서 Aiven MySQL과 R2를 환경변수로 받고, 운영 DB에는 선호도·시설·지역 기준 데이터만 반복 실행 가능한 `data-prod.sql`로 넣습니다. 개발용 `data.sql`의 샘플 사용자·코스는 운영에 적재하지 않으며 `.dockerignore`에서 로컬 `.env`를 제외합니다. |
