package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.controller.user.request.ReviewTravelCompanionRequest
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.TravelCompanion
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TravelCompanionRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.util.Optional

class TravelCompanionServiceTest {
    private val companionRepository = mock(TravelCompanionRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val storageRepository = mock(ObjectStorageRepository::class.java)
    private val service =
        TravelCompanionService(
            companionRepository,
            participantRepository,
            roomRepository,
            userRepository,
            storageRepository,
        )

    @Test
    fun `같이 완료한 여행의 사용자에게 매너점수와 한줄평을 남긴다`() {
        val owner = user(1L, "여행자")
        val target = user(2L, "동행자")
        val room = mock(ChatRoom::class.java)
        val record = TravelCompanion(id = 3L, owner = owner, companion = target, chatRoom = room)
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(true)
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 2L)).thenReturn(true)
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L)).thenReturn(emptyList())
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(room.id).thenReturn(10L)
        `when`(room.hasCompletedTrip(LocalDate.now())).thenReturn(true)
        `when`(companionRepository.findByOwnerIdAndCompanionIdAndChatRoomId(1L, 2L, 10L)).thenReturn(record)
        `when`(userRepository.findByIdForUpdate(2L)).thenReturn(target)
        `when`(companionRepository.averageMannerScoreByCompanionId(2L)).thenReturn(4.5)

        val response = service.reviewCompanion(1L, 10L, 2L, ReviewTravelCompanionRequest(5, "덕분에 즐거웠어요"))

        assertEquals(5, response.mannerScore)
        assertEquals("덕분에 즐거웠어요", response.oneLineReview)
        assertEquals(4.5, target.mannerRating)
    }

    @Test
    fun `채팅방 참가자가 아니면 동행자를 조회할 수 없다`() {
        val room = mock(ChatRoom::class.java)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))

        val exception =
            assertThrows(BaseException::class.java) {
                service.getTripCompanions(1L, 10L)
            }

        assertEquals(ErrorCode.CHAT_ROOM_NOT_PARTICIPANT, exception.errorCode)
        verifyNoInteractions(companionRepository, userRepository)
    }

    @Test
    fun `참가한 여행이 아직 끝나지 않았으면 완료 전용 기능을 사용할 수 없다`() {
        val room = mock(ChatRoom::class.java)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(true)

        val exception =
            assertThrows(BaseException::class.java) {
                service.getTripCompanions(1L, 10L)
            }

        assertEquals(ErrorCode.TRIP_NOT_COMPLETED, exception.errorCode)
        verifyNoInteractions(companionRepository, userRepository)
    }

    @Test
    fun `완료 여행의 모든 참가자를 방향별 동행 기록으로 수집하되 기존 기록은 건너뛴다`() {
        val first = user(1L, "첫째")
        val second = user(2L, "둘째")
        val room = completedRoom(10L, "경주 여행", LocalDate.now().minusDays(2))
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(
                listOf(
                    ChatRoomParticipant(chatRoom = room, user = first, role = ChatParticipantRole.HOST),
                    ChatRoomParticipant(chatRoom = room, user = second, role = ChatParticipantRole.MEMBER),
                ),
            )
        `when`(companionRepository.existsByOwnerIdAndCompanionIdAndChatRoomId(1L, 2L, 10L)).thenReturn(true)

        service.collectCompletedTrip(room)

        verify(companionRepository, org.mockito.Mockito.never()).save(
            org.mockito.ArgumentMatchers.argThat { it.owner.id == 1L },
        )
        verify(companionRepository).save(
            org.mockito.ArgumentMatchers.argThat { it.owner.id == 2L && it.companion.id == 1L },
        )
    }

    @Test
    fun `완료 여행 동행자 목록은 평가 여부를 포함한다`() {
        val owner = user(1L, "여행자")
        val target = user(2L, "동행자")
        val room = completedRoom(10L, "안동 여행", LocalDate.now().minusDays(2))
        val record = TravelCompanion(id = 3L, owner = owner, companion = target, chatRoom = room, mannerScore = 4)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(true)
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L)).thenReturn(emptyList())
        `when`(companionRepository.findAllByOwnerIdAndChatRoomIdOrderByIdAsc(1L, 10L)).thenReturn(listOf(record))

        val response = service.getTripCompanions(1L, 10L)

        assertEquals(true, response.single().reviewed)
        assertEquals(4, response.single().mannerScore)
    }

    @Test
    fun `자기 자신이나 여행에 없던 사용자는 동행자로 평가할 수 없다`() {
        val room = completedRoom(10L, "안동 여행", LocalDate.now().minusDays(2))
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 1L)).thenReturn(true)

        val selfException =
            assertThrows(BaseException::class.java) {
                service.reviewCompanion(1L, 10L, 1L, ReviewTravelCompanionRequest(5, null))
            }
        val outsiderException =
            assertThrows(BaseException::class.java) {
                service.reviewCompanion(1L, 10L, 2L, ReviewTravelCompanionRequest(5, null))
            }

        assertEquals(ErrorCode.FORBIDDEN, selfException.errorCode)
        assertEquals(ErrorCode.FORBIDDEN, outsiderException.errorCode)
    }

    @Test
    fun `여행 도감은 같은 동행자와 다녀온 여행을 최신순으로 묶는다`() {
        val owner = user(1L, "여행자")
        val target = user(2L, "동행자")
        val oldRoom = completedRoom(10L, "경주 여행", LocalDate.of(2026, 5, 1))
        val recentRoom = completedRoom(11L, "안동 여행", LocalDate.of(2026, 8, 1))
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(owner))
        `when`(companionRepository.findAllByOwnerId(1L))
            .thenReturn(
                listOf(
                    TravelCompanion(owner = owner, companion = target, chatRoom = oldRoom, oneLineReview = "좋았어요"),
                    TravelCompanion(owner = owner, companion = target, chatRoom = recentRoom),
                ),
            )

        val response = service.getMyTravelDex(1L)

        assertEquals(1, response.totalCount)
        assertEquals(2, response.companions.single().tripCount)
        assertEquals("안동 여행", response.companions.single().latestTripTitle)
        assertEquals(
            listOf(11L, 10L),
            response.companions
                .single()
                .memories
                .map { it.chatRoomId },
        )
    }

    private fun completedRoom(
        id: Long,
        title: String,
        startDate: LocalDate,
    ) = ChatRoom(
        id = id,
        host = user(99L, "호스트"),
        course = TravelCourse(id = id, type = TravelCourseType.PUBLIC, title = "코스"),
        roomTitle = title,
        maxParticipants = 3,
        startDate = startDate,
        endDate = startDate.plusDays(1),
        recruitmentDeadlineDate = startDate.minusDays(1),
        meetingDateTime = startDate.atStartOfDay(),
        status = ChatRoomStatus.CONFIRMED,
    )

    private fun user(
        id: Long,
        nickname: String,
    ): User =
        User(
            id = id,
            userRole = UserRole.ROLE_USER,
            userInformation = UserInformation(nickname, NicknameColor.GREEN, Gender.N),
        )
}
