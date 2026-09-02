package kr.hanchae.moyeotrip.entity.chat

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ChatRoomEntityTest {
    @Test
    fun `당일과 숙박 여행의 일수와 유형을 계산한다`() {
        val dayTrip = room()
        val overnight = room(endDate = START_DATE.plusDays(2))

        assertEquals(1, dayTrip.tripDays)
        assertEquals(0, dayTrip.tripNights)
        assertEquals(TripType.DAY_TRIP, dayTrip.tripType)
        assertEquals(3, overnight.tripDays)
        assertEquals(2, overnight.tripNights)
        assertEquals(TripType.OVERNIGHT, overnight.tripType)
    }

    @Test
    fun `모집과 여행 날짜의 잘못된 조합을 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            room(recruitmentDeadlineDate = START_DATE.plusDays(1))
        }
        assertThrows(IllegalArgumentException::class.java) { room(endDate = START_DATE) }
        assertThrows(IllegalArgumentException::class.java) { room(endDate = START_DATE.minusDays(1)) }
        assertThrows(IllegalArgumentException::class.java) { room(endDate = START_DATE.plusDays(30)) }
    }

    @Test
    fun `참가 인원과 참가비 경계를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { room(maxParticipants = 2) }
        assertThrows(IllegalArgumentException::class.java) { room(maxParticipants = 21) }
        assertThrows(IllegalArgumentException::class.java) { room(minimumParticipants = 2) }
        assertThrows(IllegalArgumentException::class.java) { room(minimumParticipants = 6, maxParticipants = 5) }
        assertThrows(IllegalArgumentException::class.java) { room(participationFee = -1) }

        assertEquals(0, room(participationFee = 0).participationFee)
        assertEquals(20, room(maxParticipants = 20).maxParticipants)
    }

    @Test
    fun `나이 제한의 범위와 순서를 검증한다`() {
        assertThrows(IllegalArgumentException::class.java) { room(minimumAge = 19) }
        assertThrows(IllegalArgumentException::class.java) { room(minimumAge = 101) }
        assertThrows(IllegalArgumentException::class.java) { room(maximumAge = 19) }
        assertThrows(IllegalArgumentException::class.java) { room(maximumAge = 101) }
        assertThrows(IllegalArgumentException::class.java) { room(minimumAge = 40, maximumAge = 30) }

        val room = room(minimumAge = 20, maximumAge = 100)
        assertEquals(20, room.minimumAge)
        assertEquals(100, room.maximumAge)
    }

    @Test
    fun `집합 좌표는 둘 다 존재하거나 둘 다 없어야 하고 유효 범위여야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { room(meetingLatitude = 91.0, meetingLongitude = 128.0) }
        assertThrows(IllegalArgumentException::class.java) { room(meetingLatitude = 36.0, meetingLongitude = 181.0) }
        assertThrows(IllegalArgumentException::class.java) { room(meetingLatitude = 36.0, meetingLongitude = null) }
        assertThrows(IllegalArgumentException::class.java) { room(meetingLatitude = null, meetingLongitude = 128.0) }
        assertThrows(IllegalArgumentException::class.java) { room(meetingDateTime = START_DATE.plusDays(1).atStartOfDay()) }

        val room = room(meetingLatitude = -90.0, meetingLongitude = 180.0)
        assertEquals(-90.0, room.meetingLatitude)
        assertEquals(180.0, room.meetingLongitude)
    }

    @Test
    fun `당일 여행은 시작 종료 시간이 필요하고 숙박 여행은 시간이 없어야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { room(dayTripStartTime = null) }
        assertThrows(IllegalArgumentException::class.java) { room(dayTripEndTime = null) }
        assertThrows(IllegalArgumentException::class.java) {
            room(dayTripStartTime = LocalTime.NOON, dayTripEndTime = LocalTime.NOON)
        }
        assertThrows(IllegalArgumentException::class.java) {
            room(dayTripStartTime = LocalTime.of(18, 0), dayTripEndTime = LocalTime.of(9, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            room(endDate = START_DATE.plusDays(1), dayTripStartTime = LocalTime.of(9, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            room(endDate = START_DATE.plusDays(1), dayTripEndTime = LocalTime.of(18, 0))
        }
    }

    @Test
    fun `집합 정보 수정도 좌표와 날짜 규칙을 검증한다`() {
        val room = room()

        assertThrows(IllegalArgumentException::class.java) {
            room.updateMeetingInfo(91.0, 128.0, null, START_DATE.atStartOfDay())
        }
        assertThrows(IllegalArgumentException::class.java) {
            room.updateMeetingInfo(36.0, 181.0, null, START_DATE.atStartOfDay())
        }
        assertThrows(IllegalArgumentException::class.java) {
            room.updateMeetingInfo(36.0, null, null, START_DATE.atStartOfDay())
        }
        assertThrows(IllegalArgumentException::class.java) {
            room.updateMeetingInfo(null, null, null, START_DATE.plusDays(1).atStartOfDay())
        }

        room.updateMeetingInfo(36.5, 128.5, "안동역", START_DATE.atTime(8, 30))
        assertEquals(36.5, room.meetingLatitude)
        assertEquals(128.5, room.meetingLongitude)
        assertEquals("안동역", room.meetingDetails)
    }

    @Test
    fun `취소 확정 보관의 채팅 생명주기를 관리한다`() {
        val room = room()
        val now = START_DATE.atTime(12, 0)

        assertTrue(room.canChat())
        room.cancel(now)
        assertEquals(ChatRoomStatus.CANCELLED, room.status)
        assertFalse(room.canChat())
        assertEquals(now.toLocalDate().plusDays(14), room.deletionScheduledDate)

        room.confirm()
        assertEquals(ChatRoomStatus.CONFIRMED, room.status)
        assertTrue(room.canChat())
        assertNull(room.deletionScheduledDate)

        room.scheduleDeletion(START_DATE.plusDays(20))
        assertEquals(START_DATE.plusDays(20), room.deletionScheduledDate)
        room.archiveChat(now)
        assertTrue(room.isChatArchived())
        assertFalse(room.canChat())
        assertNull(room.deletionScheduledDate)
    }

    @Test
    fun `확정 상태가 아니면 삭제 예약과 채팅 보관을 거부한다`() {
        val room = room()

        assertThrows(IllegalArgumentException::class.java) { room.scheduleDeletion(START_DATE.plusDays(14)) }
        assertThrows(IllegalArgumentException::class.java) { room.archiveChat(START_DATE.atStartOfDay()) }
        assertFalse(room.isChatArchived())
    }

    @Test
    fun `참가 신청 가능일과 여행 완료 여부를 상태와 날짜로 판정한다`() {
        val room = room()

        assertTrue(room.canAcceptJoinApplication(START_DATE.minusDays(2)))
        assertFalse(room.canAcceptJoinApplication(START_DATE.minusDays(1)))
        assertFalse(room.hasCompletedTrip(START_DATE.plusDays(1)))

        room.confirm()
        assertFalse(room.hasCompletedTrip(START_DATE))
        assertTrue(room.hasCompletedTrip(START_DATE.plusDays(1)))

        room.cancel(START_DATE.atStartOfDay())
        assertFalse(room.canAcceptJoinApplication(START_DATE.minusDays(2)))
    }

    @Test
    fun `모집 디데이는 마감 전과 당일만 반환한다`() {
        val room = room(recruitmentDeadlineDate = START_DATE.minusDays(3))

        assertEquals(2, room.recruitmentDDay(START_DATE.minusDays(5)))
        assertEquals(0, room.recruitmentDDay(START_DATE.minusDays(3)))
        assertNull(room.recruitmentDDay(START_DATE.minusDays(2)))
    }

    private fun room(
        endDate: LocalDate? = null,
        recruitmentDeadlineDate: LocalDate = START_DATE.minusDays(2),
        maxParticipants: Int = 5,
        minimumParticipants: Int = 3,
        participationFee: Long? = null,
        minimumAge: Int? = null,
        maximumAge: Int? = null,
        meetingLatitude: Double? = null,
        meetingLongitude: Double? = null,
        meetingDateTime: LocalDateTime = START_DATE.atTime(8, 0),
        dayTripStartTime: LocalTime? = if (endDate == null) LocalTime.of(9, 0) else null,
        dayTripEndTime: LocalTime? = if (endDate == null) LocalTime.of(18, 0) else null,
    ): ChatRoom =
        ChatRoom(
            id = 1L,
            host = User(id = 1L, userRole = UserRole.ROLE_USER),
            course = TravelCourse(id = 1L, type = TravelCourseType.CUSTOM, title = "테스트 코스"),
            roomTitle = "테스트 모임",
            maxParticipants = maxParticipants,
            minimumParticipants = minimumParticipants,
            startDate = START_DATE,
            endDate = endDate,
            recruitmentDeadlineDate = recruitmentDeadlineDate,
            dayTripStartTime = dayTripStartTime,
            dayTripEndTime = dayTripEndTime,
            meetingLatitude = meetingLatitude,
            meetingLongitude = meetingLongitude,
            meetingDateTime = meetingDateTime,
            participationFee = participationFee,
            minimumAge = minimumAge,
            maximumAge = maximumAge,
        )

    companion object {
        private val START_DATE: LocalDate = LocalDate.of(2026, 9, 20)
    }
}
