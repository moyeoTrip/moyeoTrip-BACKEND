-- BE-22 · 코스 표지를 큐레이션할 수 있도록 대표 이미지 컬럼을 둔다.
-- 값이 없으면 지금까지처럼 첫 방문지의 관광 콘텐츠 썸네일을 쓴다.
ALTER TABLE travel_courses ADD thumbnail VARCHAR2(1000 CHAR);
