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
