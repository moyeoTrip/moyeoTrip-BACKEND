package kr.hanchae.moyeotrip.entity.notification

import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationSettingTest {
    @Test
    fun `방해 금지를 켜려면 서로 다른 시작 종료 시간과 요일이 필요하다`() {
        val setting = setting()
        val validDays = setOf(DayOfWeek.MONDAY)

        assertThrows(IllegalArgumentException::class.java) {
            setting.update(ChatNotificationMode.ALL, true, true, true, true, null, LocalTime.NOON, validDays)
        }
        assertThrows(IllegalArgumentException::class.java) {
            setting.update(ChatNotificationMode.ALL, true, true, true, true, LocalTime.NOON, null, validDays)
        }
        assertThrows(IllegalArgumentException::class.java) {
            setting.update(ChatNotificationMode.ALL, true, true, true, true, LocalTime.NOON, LocalTime.NOON, validDays)
        }
        assertThrows(IllegalArgumentException::class.java) {
            setting.update(ChatNotificationMode.ALL, true, true, true, true, LocalTime.NOON, LocalTime.of(13, 0), emptySet())
        }
    }

    @Test
    fun `방해 금지를 끄면 시간 없이도 나머지 설정을 갱신한다`() {
        val setting = setting()

        setting.update(ChatNotificationMode.NONE, false, false, false, false, null, null, emptySet())

        assertEquals(ChatNotificationMode.NONE, setting.chatNotificationMode)
        assertFalse(setting.chatMessageEnabled)
        assertFalse(setting.recruitmentDeadlineEnabled)
        assertFalse(setting.socialActivityEnabled)
        assertFalse(setting.marketingEnabled)
        assertTrue(setting.doNotDisturbDays.isEmpty())
    }

    @Test
    fun `알림 유형별 수신 설정을 적용한다`() {
        val setting = setting()
        setting.update(ChatNotificationMode.NONE, false, false, false, false, null, null, emptySet())

        assertFalse(setting.allows(NotificationType.CHAT_MESSAGE_RECEIVED))
        assertFalse(setting.allows(NotificationType.RECRUITMENT_DEADLINE))
        assertFalse(setting.allows(NotificationType.FRIEND_REQUEST))
        assertFalse(setting.allows(NotificationType.FRIEND_ACCEPTED))
        assertFalse(setting.allows(NotificationType.FEED_LIKE))
        assertFalse(setting.allows(NotificationType.MARKETING))
        assertTrue(setting.allows(NotificationType.CHAT_ROOM_CREATED))
        assertTrue(setting.allows(NotificationType.CHAT_ROOM_KICKED))
        assertTrue(setting.allows(NotificationType.TRAVEL_COURSE_UPDATED))
        assertTrue(setting.allows(NotificationType.MEETING_INFO_UPDATED))

        setting.updateMarketingEnabled(true)
        assertTrue(setting.allows(NotificationType.MARKETING))
    }

    @Test
    fun `같은 날 안에서 시작 이상 종료 미만일 때 방해 금지한다`() {
        val setting = setting()
        setting.update(
            ChatNotificationMode.ALL,
            true,
            true,
            true,
            true,
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            setOf(DayOfWeek.MONDAY),
        )

        assertFalse(setting.isDoNotDisturbing(mondayAt(8, 59)))
        assertTrue(setting.isDoNotDisturbing(mondayAt(9, 0)))
        assertTrue(setting.isDoNotDisturbing(mondayAt(17, 59)))
        assertFalse(setting.isDoNotDisturbing(mondayAt(18, 0)))
        assertFalse(setting.isDoNotDisturbing(mondayAt(10, 0).plusDays(1)))
    }

    @Test
    fun `자정을 넘기는 방해 금지는 시작 요일과 다음 날 새벽을 포함한다`() {
        val setting = setting()
        setting.update(
            ChatNotificationMode.MENTIONS_AND_REPLIES,
            true,
            true,
            true,
            true,
            LocalTime.of(22, 0),
            LocalTime.of(7, 0),
            setOf(DayOfWeek.MONDAY),
        )

        assertTrue(setting.chatMessageEnabled)
        assertTrue(setting.isDoNotDisturbing(mondayAt(22, 0)))
        assertTrue(setting.isDoNotDisturbing(mondayAt(23, 59)))
        assertTrue(setting.isDoNotDisturbing(mondayAt(1, 0).plusDays(1)))
        assertFalse(setting.isDoNotDisturbing(mondayAt(7, 0).plusDays(1)))
        assertFalse(setting.isDoNotDisturbing(mondayAt(21, 0)))
    }

    @Test
    fun `방해 금지가 꺼졌거나 내부 시간이 없으면 방해 금지하지 않는다`() {
        val disabled = setting()
        assertFalse(disabled.isDoNotDisturbing(mondayAt(12, 0)))

        val missingStart = setting(doNotDisturbEnabled = true, endTime = LocalTime.NOON)
        val missingEnd = setting(doNotDisturbEnabled = true, startTime = LocalTime.NOON)
        assertFalse(missingStart.isDoNotDisturbing(mondayAt(12, 0)))
        assertFalse(missingEnd.isDoNotDisturbing(mondayAt(12, 0)))
    }

    private fun setting(
        doNotDisturbEnabled: Boolean = false,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
    ) = NotificationSetting(
        user = User(id = 1L, userRole = UserRole.ROLE_USER),
        doNotDisturbEnabled = doNotDisturbEnabled,
        doNotDisturbStartTime = startTime,
        doNotDisturbEndTime = endTime,
    )

    private fun mondayAt(
        hour: Int,
        minute: Int,
    ): LocalDateTime = LocalDate.of(2026, 8, 31).atTime(hour, minute)
}
