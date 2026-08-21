package kr.hanchae.moyeotrip.entity.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(
    name = "chat_room_notification_settings",
    uniqueConstraints = [UniqueConstraint(name = "uk_chat_room_notification_user_room", columnNames = ["user_id", "chat_room_id"])],
)
class ChatRoomNotificationSetting(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    @Column(nullable = false, columnDefinition = "NUMBER(1)")
    var enabled: Boolean = true,
) : BaseModifiableEntity() {
    fun update(enabled: Boolean) {
        this.enabled = enabled
    }
}
