package kr.hanchae.moyeotrip.entity.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(name = "chat_messages")
class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", updatable = false)
    val sender: User? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    val type: ChatMessageType,
    @Column(nullable = false, length = 1000)
    val content: String,
    @Column(name = "image_url", length = 1000)
    val imageUrl: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tourism_content_id", updatable = false)
    val tourismContent: TourismContent? = null,
    @Column(name = "shared_latitude")
    val sharedLatitude: Double? = null,
    @Column(name = "shared_longitude")
    val sharedLongitude: Double? = null,
    @Column(name = "location_name", length = 100)
    val locationName: String? = null,
    @Column(name = "poll_anonymous", columnDefinition = "NUMBER(1)")
    val pollAnonymous: Boolean? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id", updatable = false)
    val replyTo: ChatMessage? = null,
    @Column(name = "system_event_key", length = 30, updatable = false)
    val systemEventKey: String? = null,
) : BaseTimeEntity() {
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "chat_message_mentions",
        joinColumns = [JoinColumn(name = "message_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")],
    )
    private val mentionedUserEntities: MutableSet<User> = linkedSetOf()

    val mentionedUsers: Set<User>
        get() = mentionedUserEntities.toSet()

    fun mention(users: Collection<User>) {
        mentionedUserEntities.addAll(users)
    }
}

enum class ChatMessageType {
    USER,
    SYSTEM,
    IMAGE,
    TOURISM_CONTENT,
    LOCATION,
    POLL,
    SETTLEMENT_MEMO,
}
