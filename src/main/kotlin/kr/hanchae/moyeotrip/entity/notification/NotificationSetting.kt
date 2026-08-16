package kr.hanchae.moyeotrip.entity.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(name = "notification_settings")
class NotificationSetting(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    val user: User,
    @Column(name = "chat_message_enabled", nullable = false)
    var chatMessageEnabled: Boolean = true,
    @Column(name = "recruitment_deadline_enabled", nullable = false)
    var recruitmentDeadlineEnabled: Boolean = true,
    @Column(name = "social_activity_enabled", nullable = false)
    var socialActivityEnabled: Boolean = true,
    @Column(name = "marketing_enabled", nullable = false)
    var marketingEnabled: Boolean = true,
) : BaseModifiableEntity() {
    fun update(
        chatMessageEnabled: Boolean,
        recruitmentDeadlineEnabled: Boolean,
        socialActivityEnabled: Boolean,
        marketingEnabled: Boolean,
    ) {
        this.chatMessageEnabled = chatMessageEnabled
        this.recruitmentDeadlineEnabled = recruitmentDeadlineEnabled
        this.socialActivityEnabled = socialActivityEnabled
        this.marketingEnabled = marketingEnabled
    }

    fun allows(type: NotificationType): Boolean =
        when (type) {
            NotificationType.CHAT_MESSAGE_RECEIVED -> chatMessageEnabled
            NotificationType.RECRUITMENT_DEADLINE -> recruitmentDeadlineEnabled
            NotificationType.FRIEND_REQUEST, NotificationType.FEED_LIKE -> socialActivityEnabled
            NotificationType.MARKETING -> marketingEnabled
            NotificationType.CHAT_ROOM_CREATED,
            NotificationType.TRAVEL_COURSE_UPDATED,
            NotificationType.MEETING_INFO_UPDATED,
            -> true
        }
}
