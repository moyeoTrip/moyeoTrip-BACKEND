package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
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
    private lateinit var chatMessageRepository: ChatMessageRepository

    @Autowired
    private lateinit var participantRepository: ChatRoomParticipantRepository

    @Nested
    inner class SearchRoomQueries {
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
