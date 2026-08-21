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
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(name = "chat_room_kick_histories")
class ChatRoomKickHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoomId: Long,
    @Column(name = "room_title", nullable = false, updatable = false, length = 100)
    val roomTitle: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kicked_user_id", nullable = false, updatable = false)
    val kickedUser: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kicked_by_id", nullable = false, updatable = false)
    val kickedBy: User,
    @Column(nullable = false, updatable = false, length = 500)
    val reason: String,
) : BaseTimeEntity()
