package kr.hanchae.moyeotrip.entity.chat

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
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(name = "chat_poll_options")
class ChatPollOption(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, updatable = false)
    val message: ChatMessage,
    @Column(name = "option_text", nullable = false, length = 100)
    val text: String,
    @Column(name = "option_sequence", nullable = false)
    val sequence: Int,
) : BaseTimeEntity()

@Entity
@Table(
    name = "chat_poll_votes",
    uniqueConstraints = [UniqueConstraint(name = "uk_chat_poll_vote_message_user", columnNames = ["message_id", "user_id"])],
)
class ChatPollVote(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, updatable = false)
    val message: ChatMessage,
    option: ChatPollOption,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
) : BaseTimeEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    var option: ChatPollOption = option
        protected set

    fun changeOption(option: ChatPollOption) {
        this.option = option
    }
}
