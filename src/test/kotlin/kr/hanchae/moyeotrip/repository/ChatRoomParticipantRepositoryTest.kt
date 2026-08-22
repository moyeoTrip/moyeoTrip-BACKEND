package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ChatRoomParticipantRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var participantRepository: ChatRoomParticipantRepository

    @Nested
    inner class CompletedTripQueries {
        @Test
        fun `완료된 확정 여행만 집계한다`() {
            val user = savedUser()
            val host = savedUser()
            val course = savedCourse()
            val completedRoom =
                savedRoom(
                    host = host,
                    course = course,
                    startDate = LocalDate.now().minusDays(2),
                    status = ChatRoomStatus.CONFIRMED,
                )
            val futureRoom =
                savedRoom(
                    host = host,
                    course = course,
                    startDate = LocalDate.now().plusDays(2),
                    status = ChatRoomStatus.CONFIRMED,
                )
            val recruitingPastRoom =
                savedRoom(
                    host = host,
                    course = course,
                    startDate = LocalDate.now().minusDays(2),
                )
            participantRepository.saveAndFlush(
                ChatRoomParticipant(
                    chatRoom = completedRoom,
                    user = user,
                    role = ChatParticipantRole.MEMBER,
                ),
            )
            participantRepository.saveAndFlush(
                ChatRoomParticipant(
                    chatRoom = futureRoom,
                    user = user,
                    role = ChatParticipantRole.MEMBER,
                ),
            )
            participantRepository.saveAndFlush(
                ChatRoomParticipant(
                    chatRoom = recruitingPastRoom,
                    user = user,
                    role = ChatParticipantRole.MEMBER,
                ),
            )

            assertEquals(1, participantRepository.countCompletedTrips(user.id))
            assertTrue(participantRepository.hasCompletedTrip(completedRoom.id, user.id, LocalDate.now()))
            assertFalse(participantRepository.hasCompletedTrip(futureRoom.id, user.id, LocalDate.now()))
        }
    }
}
