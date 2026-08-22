package kr.hanchae.moyeotrip.entity.chat

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
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(
    name = "chat_room_participants",
    uniqueConstraints = [UniqueConstraint(name = "uk_chat_participant_room_user", columnNames = ["chat_room_id", "user_id"])],
)
class ChatRoomParticipant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: ChatParticipantRole,
    @Column(name = "last_read_message_id", nullable = false)
    var lastReadMessageId: Long = 0L,
) : BaseTimeEntity() {
    fun readThrough(messageId: Long) { // 사용자 읽은 내용 업데이트
        if (messageId > lastReadMessageId) lastReadMessageId = messageId
    }
}

@Schema(
    description = "채팅방 참가자 역할. HOST=채팅방 생성자·관리자, MEMBER=일반 참가자",
    allowableValues = ["HOST", "MEMBER"],
)
enum class ChatParticipantRole {
    HOST,
    MEMBER,
}
