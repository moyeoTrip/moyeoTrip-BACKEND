package kr.hanchae.moyeotrip.entity.notification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.user.User
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
class Notification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    val recipient: User,
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40, updatable = false)
    val type: NotificationType,
    @Column(nullable = false, length = 500, updatable = false)
    val content: String,
    @Column(name = "chat_room_id", updatable = false)
    val chatRoomId: Long? = null,
    @Column(name = "reference_id", nullable = false, updatable = false)
    val referenceId: Long,
) : BaseTimeEntity() {
    @Column(name = "read_datetime")
    var readDateTime: LocalDateTime? = null
        protected set

    fun markRead() {
        if (readDateTime == null) readDateTime = LocalDateTime.now()
    }
}

@Schema(
    description =
        "알림 유형. CHAT_ROOM_CREATED=채팅방 생성, CHAT_ROOM_KICKED=강퇴, CHAT_MESSAGE_RECEIVED=채팅 메시지, " +
            "TRAVEL_COURSE_UPDATED=여행 코스 변경, MEETING_INFO_UPDATED=집합 정보 변경, RECRUITMENT_DEADLINE=모집 마감 임박, " +
            "FRIEND_REQUEST=친구 요청, FRIEND_ACCEPTED=친구 수락, FEED_LIKE=피드 좋아요, MARKETING=마케팅 알림",
    allowableValues = [
        "CHAT_ROOM_CREATED", "CHAT_ROOM_KICKED", "CHAT_MESSAGE_RECEIVED", "TRAVEL_COURSE_UPDATED", "MEETING_INFO_UPDATED",
        "RECRUITMENT_DEADLINE", "FRIEND_REQUEST", "FRIEND_ACCEPTED", "FEED_LIKE", "MARKETING",
    ],
)
enum class NotificationType {
    CHAT_ROOM_CREATED,
    CHAT_ROOM_KICKED,
    CHAT_MESSAGE_RECEIVED,
    TRAVEL_COURSE_UPDATED,
    MEETING_INFO_UPDATED,
    RECRUITMENT_DEADLINE,
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    FEED_LIKE,
    MARKETING,
}
