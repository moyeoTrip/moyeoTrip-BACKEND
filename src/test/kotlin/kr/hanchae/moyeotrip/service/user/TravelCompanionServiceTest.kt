package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.controller.user.request.ReviewTravelCompanionRequest
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
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
