package kr.hanchae.moyeotrip.entity.notification

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.user.User
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "notification_settings")
class NotificationSetting(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    val user: User,
    @Enumerated(EnumType.STRING)
    @Column(name = "chat_notification_mode", nullable = false, length = 30)
    var chatNotificationMode: ChatNotificationMode = ChatNotificationMode.ALL,
    @Column(name = "recruitment_deadline_enabled", nullable = false, columnDefinition = "NUMBER(1)")
    var recruitmentDeadlineEnabled: Boolean = true,
    @Column(name = "social_activity_enabled", nullable = false, columnDefinition = "NUMBER(1)")
    var socialActivityEnabled: Boolean = true,
    @Column(name = "marketing_enabled", nullable = false, columnDefinition = "NUMBER(1)")
    var marketingEnabled: Boolean = true,
    @Column(name = "do_not_disturb_enabled", nullable = false, columnDefinition = "NUMBER(1)")
    var doNotDisturbEnabled: Boolean = false,
    @Column(name = "do_not_disturb_start_time")
    var doNotDisturbStartTime: LocalTime? = null,
    @Column(name = "do_not_disturb_end_time")
    var doNotDisturbEndTime: LocalTime? = null,
) : BaseModifiableEntity() {
    @ElementCollection
    @CollectionTable(name = "notification_do_not_disturb_days", joinColumns = [JoinColumn(name = "setting_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private val selectedDoNotDisturbDays: MutableSet<DayOfWeek> = linkedSetOf()

    val doNotDisturbDays: Set<DayOfWeek>
        get() = selectedDoNotDisturbDays.toSet()

    fun update(
        chatNotificationMode: ChatNotificationMode,
        recruitmentDeadlineEnabled: Boolean,
        socialActivityEnabled: Boolean,
        marketingEnabled: Boolean,
        doNotDisturbEnabled: Boolean,
        doNotDisturbStartTime: LocalTime?,
        doNotDisturbEndTime: LocalTime?,
        doNotDisturbDays: Set<DayOfWeek>,
    ) {
        require(
            !doNotDisturbEnabled ||
                (
                    doNotDisturbStartTime != null &&
                        doNotDisturbEndTime != null &&
                        doNotDisturbStartTime != doNotDisturbEndTime &&
                        doNotDisturbDays.isNotEmpty()
                ),
        )
        this.chatNotificationMode = chatNotificationMode
        this.recruitmentDeadlineEnabled = recruitmentDeadlineEnabled
        this.socialActivityEnabled = socialActivityEnabled
        this.marketingEnabled = marketingEnabled
        this.doNotDisturbEnabled = doNotDisturbEnabled
        this.doNotDisturbStartTime = doNotDisturbStartTime
        this.doNotDisturbEndTime = doNotDisturbEndTime
        selectedDoNotDisturbDays.clear()
        selectedDoNotDisturbDays.addAll(doNotDisturbDays)
    }

    fun allows(type: NotificationType): Boolean =
        when (type) {
            NotificationType.CHAT_MESSAGE_RECEIVED -> chatNotificationMode != ChatNotificationMode.NONE
            NotificationType.RECRUITMENT_DEADLINE -> recruitmentDeadlineEnabled
            NotificationType.FRIEND_REQUEST, NotificationType.FRIEND_ACCEPTED, NotificationType.FEED_LIKE -> socialActivityEnabled
            NotificationType.MARKETING -> marketingEnabled
            NotificationType.CHAT_ROOM_CREATED,
            NotificationType.CHAT_ROOM_KICKED,
            NotificationType.TRAVEL_COURSE_UPDATED,
            NotificationType.MEETING_INFO_UPDATED,
            -> true
        }

    val chatMessageEnabled: Boolean
        get() = chatNotificationMode != ChatNotificationMode.NONE

    fun isDoNotDisturbing(now: LocalDateTime): Boolean {
        if (!doNotDisturbEnabled) return false
        val start = doNotDisturbStartTime ?: return false
        val end = doNotDisturbEndTime ?: return false
        val time = now.toLocalTime()
        return if (start < end) {
            now.dayOfWeek in selectedDoNotDisturbDays && time >= start && time < end
        } else {
            (time >= start && now.dayOfWeek in selectedDoNotDisturbDays) ||
                (time < end && now.dayOfWeek.minus(1) in selectedDoNotDisturbDays)
        }
    }
}

enum class ChatNotificationMode {
    ALL,
    MENTIONS_AND_REPLIES,
    NONE,
}
