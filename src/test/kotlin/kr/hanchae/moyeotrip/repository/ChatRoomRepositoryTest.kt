package kr.hanchae.moyeotrip.repository

import jakarta.persistence.EntityManager
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCoursePlace
import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

class ChatRoomRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository

    @Autowired
    private lateinit var participantRepository: ChatRoomParticipantRepository

    @Nested
    inner class SearchRoomQueries {
        @Test
        fun `지도 경계 안에 집합 좌표가 있는 모집 방만 조회한다`() {
            val me = savedUser()
            val host = savedUser()
            val course = savedCourse()
            val visible = savedRoom(host, course, title = "지도 조회 대상")
            visible.updateMeetingInfo(36.0322, 129.3747, "포항역", visible.meetingDateTime)
            chatRoomRepository.saveAndFlush(visible)
            val outside = savedRoom(host, course, title = "지도 범위 밖")
            outside.updateMeetingInfo(37.5665, 126.9780, "서울역", outside.meetingDateTime)
            chatRoomRepository.saveAndFlush(outside)
            savedRoom(host, course, title = "집합 좌표 없음")

            val rooms =
                chatRoomRepository.findMapRooms(
                    userId = me.id,
                    blockedUserIds = listOf(-1L),
                    today = LocalDate.now(),
                    minimumLatitude = 35.9,
                    maximumLatitude = 36.1,
                    minimumLongitude = 129.2,
                    maximumLongitude = 129.5,
                    crossesDateLine = false,
                )

            assertEquals(listOf(visible.id), rooms.map { it.id })
        }

        @Test
        fun `검색어와 차단 및 참가 상태를 적용해 모집 중인 방을 조회한다`() {
            val me = savedUser()
            val host = savedUser()
            val blockedHost = savedUser()
            val blockedMember = savedUser()
            val course = savedCourse()
            val today = LocalDate.now()
            val visible = savedRoom(host, course, title = "경주 야경 산책", startDate = today.plusDays(3))
            savedRoom(blockedHost, course, title = "경주 차단 호스트", startDate = today.plusDays(3))
            val blockedMemberRoom = savedRoom(host, course, title = "경주 차단 멤버", startDate = today.plusDays(3))
            val joinedRoom = savedRoom(host, course, title = "경주 이미 참가", startDate = today.plusDays(3))
            val confirmedRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "경주 확정",
                    startDate = today.plusDays(3),
                    status = ChatRoomStatus.CONFIRMED,
                )
            savedRoom(host, course, title = "부산 여행", startDate = today.plusDays(3))
            savedRoom(
                host = host,
                course = course,
                title = "경주 마감",
                startDate = today.plusDays(3),
                recruitmentDeadlineDate = today.minusDays(1),
            )
            participantRepository.saveAndFlush(
                ChatRoomParticipant(
                    chatRoom = blockedMemberRoom,
                    user = blockedMember,
                    role = ChatParticipantRole.MEMBER,
                ),
            )
            participantRepository.saveAndFlush(
                ChatRoomParticipant(
                    chatRoom = joinedRoom,
                    user = me,
                    role = ChatParticipantRole.MEMBER,
                ),
            )

            val rooms =
                chatRoomRepository.searchRooms(
                    userId = me.id,
                    blockedUserIds = listOf(blockedHost.id, blockedMember.id),
                    keyword = "경주",
                    today = today,
                    pageable = PageRequest.of(0, 20),
                )

            assertEquals(listOf(visible.id), rooms.map { it.id })
            assertFalse(rooms.map { it.id }.contains(confirmedRoom.id))
        }

        @Test
        fun `소개 코스 태그 방문지와 지역 주소 검색어도 모집 중인 방을 조회한다`() {
            val me = savedUser()
            val host = savedUser()
            val course =
                travelCourseRepository.saveAndFlush(
                    TravelCourse(
                        type = TravelCourseType.CUSTOM,
                        owner = host,
                        title = "청송 주왕산 트레킹",
                    ),
                )
            val tagKeyword = "QA힐링${System.nanoTime()}"
            val tag = travelCourseTagRepository.saveAndFlush(TravelCourseTag(name = tagKeyword))
            course.addTags(listOf(tag))
            travelCourseRepository.saveAndFlush(course)
            val contentType = tourismContentTypeRepository.saveAndFlush(TourismContentType(99, "QA 관광지 ${System.nanoTime()}"))
            val content =
                tourismContentRepository.saveAndFlush(
                    TourismContent(
                        contentId = 999_001L,
                        contentType = contentType,
                        title = "주왕산 대전사",
                        address1 = "경상북도 청송군 부동면",
                    ),
                )
            travelCoursePlaceRepository.saveAndFlush(
                TravelCoursePlace(
                    course = course,
                    tourismContent = content,
                    dayNumber = 1,
                    sequence = 1,
                ),
            )
            val room =
                savedRoom(
                    host = host,
                    course = course,
                    title = "제목에는 검색어가 없는 모임",
                    description = "사진을 천천히 찍으며 걸어요",
                    startDate = LocalDate.now().plusDays(3),
                )

            listOf("사진", "청송", tagKeyword, "대전사", "부동면").forEach { keyword ->
                val rooms =
                    chatRoomRepository.searchRooms(
                        userId = me.id,
                        blockedUserIds = listOf(-1L),
                        keyword = keyword,
                        today = LocalDate.now(),
                        pageable = PageRequest.of(0, 20),
                    )

                assertEquals(listOf(room.id), rooms.map { it.id }, "$keyword 검색 결과")
            }
        }

        @Test
        fun `제목 검색과 코스 태그 검색은 각각 해당 조건에만 일치하는 모집 방을 조회한다`() {
            val me = savedUser()
            val host = savedUser()
            val titleCourse = savedCourse()
            val tagKeyword = "QA 태그 ${System.nanoTime()}"
            val tag = travelCourseTagRepository.saveAndFlush(TravelCourseTag(name = tagKeyword))
            val taggedCourse = TravelCourse(type = TravelCourseType.CUSTOM, owner = host, title = "태그 코스")
            taggedCourse.addTags(listOf(tag))
            travelCourseRepository.saveAndFlush(taggedCourse)
            val titleMatched = savedRoom(host, titleCourse, title = "바다 제목 모임", startDate = LocalDate.now().plusDays(3))
            val tagMatched = savedRoom(host, taggedCourse, title = "제목에는 태그가 없는 모임", startDate = LocalDate.now().plusDays(3))

            val titleRooms =
                chatRoomRepository.searchRoomsByTitle(
                    userId = me.id,
                    blockedUserIds = listOf(-1L),
                    keyword = "바다",
                    today = LocalDate.now(),
                    pageable = PageRequest.of(0, 20),
                )
            val tagRooms =
                chatRoomRepository.searchRoomsByCourseTag(
                    userId = me.id,
                    blockedUserIds = listOf(-1L),
                    tagId = tag.id,
                    today = LocalDate.now(),
                    pageable = PageRequest.of(0, 20),
                )

            assertEquals(listOf(titleMatched.id), titleRooms.map { it.id })
            assertEquals(listOf(tagMatched.id), tagRooms.map { it.id })
        }

        @Test
        fun `공개 코스로 만든 모집 방은 차단 및 참가 상태를 적용해 조회한다`() {
            val me = savedUser()
            val host = savedUser()
            val blockedHost = savedUser()
            val blockedMember = savedUser()
            val course = savedCourse()
            val otherCourse = savedCourse()
            val today = LocalDate.now()
            val visible = savedRoom(host, course, title = "조회 대상", startDate = today.plusDays(3))
            val blockedMemberRoom = savedRoom(host, course, title = "차단 멤버", startDate = today.plusDays(3))
            val joinedRoom = savedRoom(host, course, title = "이미 참가", startDate = today.plusDays(3))
            savedRoom(blockedHost, course, title = "차단 호스트", startDate = today.plusDays(3))
            savedRoom(host, course, title = "모집 마감", startDate = today.plusDays(3), recruitmentDeadlineDate = today.minusDays(1))
            savedRoom(host, otherCourse, title = "다른 코스", startDate = today.plusDays(3))
            participantRepository.saveAndFlush(
                ChatRoomParticipant(
                    chatRoom = blockedMemberRoom,
                    user = blockedMember,
                    role = ChatParticipantRole.MEMBER,
                ),
            )
            participantRepository.saveAndFlush(
                ChatRoomParticipant(
                    chatRoom = joinedRoom,
                    user = me,
                    role = ChatParticipantRole.MEMBER,
                ),
            )

            val rooms =
                chatRoomRepository.findRecruitingRoomsByPublicCourseId(
                    userId = me.id,
                    blockedUserIds = listOf(blockedHost.id, blockedMember.id),
                    courseId = course.id,
                    today = today,
                    pageable = PageRequest.of(0, 20),
                )

            assertEquals(listOf(visible.id), rooms.map { it.id })
        }
    }

    @Nested
    inner class StartEventQueries {
        @Test
        fun `여행 시작 시스템 메시지가 없는 방만 조회한다`() {
            val host = savedUser()
            val course = savedCourse()
            val today = LocalDate.now()
            val startingRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "오늘 시작",
                    startDate = today,
                    status = ChatRoomStatus.CONFIRMED,
                )
            val startedRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "이미 시작",
                    startDate = today,
                    status = ChatRoomStatus.CONFIRMED,
                )
            val recruitingRoom = savedRoom(host, course, title = "모집 중", startDate = today)
            chatMessageRepository.saveAndFlush(
                ChatMessage(
                    chatRoom = startedRoom,
                    type = ChatMessageType.SYSTEM,
                    content = "여행이 시작됐어요",
                    systemEventKey = "TRIP_STARTED",
                ),
            )

            val rooms =
                chatRoomRepository.findAllStartingRoomsWithoutSystemEvent(
                    ChatRoomStatus.CONFIRMED,
                    today,
                    "TRIP_STARTED",
                )

            assertEquals(setOf(startingRoom.id), rooms.map { it.id }.toSet())
            assertFalse(rooms.map { it.id }.contains(recruitingRoom.id))
        }

        @Test
        fun `비관적 락 대상 방을 ID로 조회한다`() {
            val host = savedUser()
            val room = savedRoom(host, savedCourse())

            assertEquals(room.id, chatRoomRepository.findByIdForUpdate(room.id)?.id)
            assertNull(chatRoomRepository.findByIdForUpdate(Long.MAX_VALUE))
        }
    }

    @Nested
    inner class LifecycleQueries {
        @Test
        fun `QA용 완료 처리는 여행을 확정하고 과거 일정으로 갱신한다`() {
            val host = savedUser()
            val course = savedCourse()
            val completedEndDate = LocalDate.now().minusDays(1)
            val dayTrip = savedRoom(host, course, startDate = LocalDate.now().plusDays(3))

            assertEquals(1, chatRoomRepository.completeForTest(dayTrip.id, completedEndDate, completedEndDate.minusDays(1), null))

            entityManager.clear()
            val completedRoom = chatRoomRepository.findById(dayTrip.id).orElseThrow()
            assertEquals(ChatRoomStatus.CONFIRMED, completedRoom.status)
            assertEquals(completedEndDate, completedRoom.startDate)
            assertEquals(completedEndDate.minusDays(1), completedRoom.recruitmentDeadlineDate)
            assertNull(completedRoom.endDate)
            assertTrue(completedRoom.hasCompletedTrip())
        }

        @Test
        fun `모집 만료와 삭제 예정 방을 각각 조회한다`() {
            val host = savedUser()
            val course = savedCourse()
            val today = LocalDate.now()
            val expiredRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "만료 모집",
                    startDate = today.plusDays(1),
                    recruitmentDeadlineDate = today.minusDays(1),
                )
            val deletionDueRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "삭제 예정",
                    startDate = today.minusDays(3),
                    status = ChatRoomStatus.CONFIRMED,
                )
            deletionDueRoom.scheduleDeletion(today)
            chatRoomRepository.saveAndFlush(deletionDueRoom)
            val deletionFutureRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "삭제 대기",
                    startDate = today.minusDays(3),
                    status = ChatRoomStatus.CONFIRMED,
                )
            deletionFutureRoom.scheduleDeletion(today.plusDays(1))
            chatRoomRepository.saveAndFlush(deletionFutureRoom)

            assertTrue(
                chatRoomRepository
                    .findAllExpiredRecruitingRoomsForUpdate(ChatRoomStatus.RECRUITING, today)
                    .map { it.id }
                    .contains(expiredRoom.id),
            )
            assertTrue(
                chatRoomRepository
                    .findAllDeletionDueRoomsForUpdate(today)
                    .map { it.id }
                    .contains(deletionDueRoom.id),
            )
            assertFalse(
                chatRoomRepository
                    .findAllDeletionDueRoomsForUpdate(today)
                    .map { it.id }
                    .contains(deletionFutureRoom.id),
            )
        }

        @Test
        fun `완료 방의 보관과 삭제 예약 상태를 구분한다`() {
            val host = savedUser()
            val course = savedCourse()
            val today = LocalDate.now()
            val completedRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "여행 완료",
                    startDate = today.minusDays(3),
                    status = ChatRoomStatus.CONFIRMED,
                )
            val deletionDueRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "삭제 예정",
                    startDate = today.minusDays(3),
                    status = ChatRoomStatus.CONFIRMED,
                )
            deletionDueRoom.scheduleDeletion(today)
            chatRoomRepository.saveAndFlush(deletionDueRoom)
            val archivedRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "대화 보관",
                    startDate = today.minusDays(3),
                    status = ChatRoomStatus.CONFIRMED,
                )
            archivedRoom.archiveChat(LocalDateTime.now())
            chatRoomRepository.saveAndFlush(archivedRoom)
            val futureRoom =
                savedRoom(
                    host = host,
                    course = course,
                    title = "미래 여행",
                    startDate = today.plusDays(3),
                    status = ChatRoomStatus.CONFIRMED,
                )

            val roomsWithoutSchedule =
                chatRoomRepository.findAllCompletedRoomsWithoutDeletionScheduleForUpdate(ChatRoomStatus.CONFIRMED, today)
            val completedRooms = chatRoomRepository.findAllCompletedConfirmedRooms(ChatRoomStatus.CONFIRMED, today)

            assertTrue(roomsWithoutSchedule.map { it.id }.contains(completedRoom.id))
            assertFalse(roomsWithoutSchedule.map { it.id }.contains(deletionDueRoom.id))
            assertFalse(roomsWithoutSchedule.map { it.id }.contains(archivedRoom.id))
            assertTrue(completedRooms.map { it.id }.containsAll(listOf(completedRoom.id, archivedRoom.id)))
            assertFalse(completedRooms.map { it.id }.contains(futureRoom.id))
            assertTrue(
                chatRoomRepository.existsCompletedHostRoom(
                    host.id,
                    course.id,
                    ChatRoomStatus.CONFIRMED,
                    today,
                ),
            )
            assertFalse(
                chatRoomRepository.existsCompletedHostRoom(
                    Long.MAX_VALUE,
                    course.id,
                    ChatRoomStatus.CONFIRMED,
                    today,
                ),
            )
        }
    }
}
