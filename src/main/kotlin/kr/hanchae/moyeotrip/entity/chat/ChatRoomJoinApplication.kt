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
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.user.User
import java.time.LocalDateTime

@Entity
@Table(name = "chat_room_join_applications")
class ChatRoomJoinApplication(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @Column(name = "application_message", nullable = false, length = 500)
    val applicationMessage: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: JoinApplicationStatus = JoinApplicationStatus.PENDING,
) : BaseTimeEntity() {
    // BE-17: 호스트의 「거절 기록」에 거절 시각을 보여주기 위해 남긴다. V64 이전에 거절된 행은 null 이다.
    @Column(name = "rejected_datetime")
    var rejectedDateTime: LocalDateTime? = null
        protected set

    fun moveToWaitlist() {
        check(status == JoinApplicationStatus.PENDING)
        status = JoinApplicationStatus.WAITLISTED
    }

    fun reject(now: LocalDateTime = LocalDateTime.now()) {
        check(status == JoinApplicationStatus.PENDING)
        status = JoinApplicationStatus.REJECTED
        rejectedDateTime = now
    }
}

@Schema(
    description = "참가 신청 상태. PENDING=호스트 승인 대기, WAITLISTED=승인됐지만 정원 초과로 대기열, REJECTED=호스트가 거절",
    allowableValues = ["PENDING", "WAITLISTED", "REJECTED"],
)
enum class JoinApplicationStatus {
    PENDING,
    WAITLISTED,
    REJECTED,
}
