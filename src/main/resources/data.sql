INSERT INTO tourism_preference (code, name, is_active)
VALUES
  ('NATURE', '자연/풍경', true),
  ('HISTORY', '역사/유적', true),
  ('CULTURE_ART', '문화/예술', true),
  ('ACTIVITY', '액티비티', true),
  ('FOOD_TOUR', '맛집 탐방', true),
  ('SHOPPING', '쇼핑', true),
  ('PHOTO_SPOT', '사진 명소', false),
  ('HEALING', '휴식/힐링', true),
  ('FESTIVAL', '축제/이벤트', true),
  ('THEME_PARK', '테마파크', true)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  is_active = VALUES(is_active);

INSERT INTO food_preference (code, name, is_active)
VALUES
  ('KOREAN', '한식', true),
  ('WESTERN', '양식', true),
  ('CHINESE', '중식', true),
  ('JAPANESE', '일식', true),
  ('CAFE_DESSERT', '카페/디저트', true),
  ('MILD', '순한 맛', false),
  ('SPICY', '매운 맛', true),
  ('VEGETARIAN', '채식', true),
  ('LOW_SODIUM', '저염식', true),
  ('LOCAL_SPECIALTY', '지역 특산 음식', true)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  is_active = VALUES(is_active);

INSERT INTO facility (code, name, is_active)
VALUES
  ('ACCESSIBLE_RESTROOM', '장애인 화장실', true),
  ('ELEVATOR', '엘리베이터', true),
  ('RAMP', '경사로', true),
  ('WHEELCHAIR_RENTAL', '휠체어 대여', true),
  ('STROLLER_RENTAL', '유모차 대여', true),
  ('NURSING_ROOM', '수유실', true),
  ('ACCESSIBLE_PARKING', '장애인 주차구역', true),
  ('REST_AREA', '휴식 공간', true),
  ('BRAILLE_BLOCK', '점자 블록', true),
  ('AUDIO_GUIDE', '음성 안내', true)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  is_active = VALUES(is_active);

INSERT INTO course_interest_keyword (code, name, is_active)
VALUES
  ('KIDS_FRIENDLY', '아이와 함께', true),
  ('SENIOR_FRIENDLY', '어르신과 함께', true),
  ('BARRIER_FREE', '무장애 여행', true),
  ('RAINY_DAY', '비 오는 날', true),
  ('FOODIE', '맛집 중심', true),
  ('RELAXING', '여유로운 일정', true),
  ('NATURE', '자연 풍경', true),
  ('HISTORY_CULTURE', '역사/문화', true),
  ('PHOTO', '사진 명소', true),
  ('SHOPPING', '쇼핑', true),
  ('ACTIVITY', '액티비티', true),
  ('BUDGET', '가성비', true)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  is_active = VALUES(is_active);

INSERT INTO region (area_code, sigungu_code, name)
VALUES
  ('1', '0', '서울'),
  ('2', '0', '인천'),
  ('3', '0', '대전'),
  ('4', '0', '대구'),
  ('5', '0', '광주'),
  ('6', '0', '부산'),
  ('7', '0', '울산'),
  ('8', '0', '세종'),
  ('31', '0', '경기'),
  ('32', '0', '강원'),
  ('33', '0', '충북'),
  ('34', '0', '충남'),
  ('35', '0', '경북'),
  ('36', '0', '경남'),
  ('37', '0', '전북'),
  ('38', '0', '전남'),
  ('39', '0', '제주')
ON DUPLICATE KEY UPDATE
  name = VALUES(name);

-- Local API smoke-test data for issue #11 course EXP APIs.
-- Sample users: 9001 is the creator, 9002 and 9003 are connected family members.
INSERT INTO users (
  user_id,
  provider,
  provider_user_id,
  email,
  nickname,
  profile_image_url,
  created_at,
  updated_at,
  deleted_at
)
VALUES
  (9001, 'KAKAO', 'local-course-user-9001', 'course-user@example.com', '코스테스터', NULL, NOW(), NOW(), NULL),
  (9002, 'KAKAO', 'local-course-user-9002', 'mom-course@example.com', '엄마', NULL, NOW(), NOW(), NULL),
  (9003, 'KAKAO', 'local-course-user-9003', 'child-course@example.com', '아이', NULL, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE
  email = VALUES(email),
  nickname = VALUES(nickname),
  profile_image_url = VALUES(profile_image_url),
  updated_at = NOW(),
  deleted_at = NULL;

INSERT INTO family_relation (
  family_relation_id,
  user_id,
  family_user_id,
  relationship,
  created_at,
  deleted_at
)
VALUES
  (9301, 9001, 9002, 'MOTHER', NOW(), NULL),
  (9302, 9001, 9003, 'CHILD', NOW(), NULL)
ON DUPLICATE KEY UPDATE
  relationship = VALUES(relationship),
  deleted_at = NULL;

INSERT INTO region (region_id, area_code, sigungu_code, name)
VALUES
  (9001, '3', '1', '대전광역시 동구')
ON DUPLICATE KEY UPDATE
  area_code = VALUES(area_code),
  sigungu_code = VALUES(sigungu_code),
  name = VALUES(name);

INSERT INTO place (
  place_id,
  content_id,
  content_type_id,
  source,
  cat1,
  region_id,
  name,
  address,
  latitude,
  longitude,
  image_url,
  created_at,
  updated_at
)
VALUES
  (9201, 'LOCAL-9201', '12', 'TOUR_API', 'NATURE', 9001, '한밭수목원', '대전광역시 서구 둔산대로 169', 36.3660000, 127.3880000, 'https://example.com/hanbat.jpg', NOW(), NOW()),
  (9202, 'LOCAL-9202', '12', 'TOUR_API', 'CULTURE', 9001, '대전근현대사전시관', '대전광역시 중구 중앙로 101', 36.3270000, 127.4210000, 'https://example.com/daejeon-history.jpg', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  source = VALUES(source),
  cat1 = VALUES(cat1),
  region_id = VALUES(region_id),
  name = VALUES(name),
  address = VALUES(address),
  latitude = VALUES(latitude),
  longitude = VALUES(longitude),
  image_url = VALUES(image_url),
  updated_at = NOW();

INSERT INTO course (
  course_id,
  creator_user_id,
  region_id,
  title,
  start_date,
  end_date,
  start_time,
  status,
  image_url,
  confirmed_at,
  created_at,
  updated_at,
  deleted_at
)
VALUES
  (9101, 9001, 9001, '수정·확정 테스트용 대전 코스', DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 11 DAY), '09:00:00', 'DRAFT', 'https://example.com/course-draft.jpg', NULL, NOW(), NOW(), NULL),
  (9102, 9001, 9001, '상세 조회 테스트용 대전 코스', DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 21 DAY), '10:00:00', 'UPCOMING', 'https://example.com/course-detail.jpg', NOW(), NOW(), NOW(), NULL),
  (9103, 9001, 9001, '삭제 테스트용 대전 코스', DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 31 DAY), '11:00:00', 'DRAFT', 'https://example.com/course-delete.jpg', NULL, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE
  creator_user_id = VALUES(creator_user_id),
  region_id = VALUES(region_id),
  title = VALUES(title),
  start_date = VALUES(start_date),
  end_date = VALUES(end_date),
  start_time = VALUES(start_time),
  status = VALUES(status),
  image_url = VALUES(image_url),
  confirmed_at = VALUES(confirmed_at),
  updated_at = NOW(),
  deleted_at = NULL;

INSERT INTO course_participant (
  course_participant_id,
  course_id,
  user_id,
  name_snapshot,
  relationship_snapshot,
  profile_image_url_snapshot,
  created_at
)
VALUES
  (9401, 9101, 9002, '엄마', 'MOTHER', NULL, NOW()),
  (9402, 9101, 9003, '아이', 'CHILD', NULL, NOW()),
  (9403, 9102, 9002, '엄마', 'MOTHER', NULL, NOW()),
  (9404, 9102, 9003, '아이', 'CHILD', NULL, NOW()),
  (9405, 9103, 9002, '엄마', 'MOTHER', NULL, NOW())
ON DUPLICATE KEY UPDATE
  name_snapshot = VALUES(name_snapshot),
  relationship_snapshot = VALUES(relationship_snapshot),
  profile_image_url_snapshot = VALUES(profile_image_url_snapshot);

INSERT IGNORE INTO course_keyword (course_id, keyword_id)
SELECT 9102, keyword_id
FROM course_interest_keyword
WHERE code IN ('BARRIER_FREE', 'NATURE', 'RELAXING');

INSERT INTO course_schedule_item (
  schedule_item_id,
  course_id,
  place_id,
  day_number,
  visit_order,
  arrival_time,
  departure_time,
  transport_mode_to_next,
  duration_minutes_to_next,
  distance_meters_to_next,
  created_at,
  updated_at
)
VALUES
  (9501, 9102, 9201, 1, 1, '10:00:00', '12:00:00', 'CAR', 25, 8400, NOW(), NOW()),
  (9502, 9102, 9202, 1, 2, '13:30:00', '15:00:00', NULL, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  place_id = VALUES(place_id),
  arrival_time = VALUES(arrival_time),
  departure_time = VALUES(departure_time),
  transport_mode_to_next = VALUES(transport_mode_to_next),
  duration_minutes_to_next = VALUES(duration_minutes_to_next),
  distance_meters_to_next = VALUES(distance_meters_to_next),
  updated_at = NOW();

INSERT IGNORE INTO course_like (user_id, course_id, created_at)
VALUES
  (9001, 9102, NOW()),
  (9002, 9102, NOW());

INSERT INTO album (
  album_id,
  course_id,
  title,
  description,
  cover_photo_id,
  created_at,
  updated_at
)
VALUES
  (9601, 9102, '상세 조회 테스트용 앨범', '코스 상세 조회에서 albumId 확인용 샘플 앨범입니다.', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  updated_at = NOW();

INSERT INTO place_accessibility (
  place_id,
  facility_id,
  status,
  source,
  verified_at,
  updated_at
)
SELECT 9201, facility_id, 'AVAILABLE', 'TOUR_API', NOW(), NOW()
FROM facility
WHERE code IN ('ACCESSIBLE_PARKING', 'REST_AREA')
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  source = VALUES(source),
  verified_at = VALUES(verified_at),
  updated_at = NOW();
