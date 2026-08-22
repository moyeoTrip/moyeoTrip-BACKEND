package kr.hanchae.moyeotrip.entity.feed

import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(
    name = "feeds",
    uniqueConstraints = [UniqueConstraint(name = "uk_feed_room_author", columnNames = ["chat_room_id", "author_id"])],
)
class Feed(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    val author: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    @Column(nullable = false, length = 500)
    val content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val visibility: FeedVisibility,
) : BaseTimeEntity() {
    @OneToMany(mappedBy = "feed", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sequence ASC")
    private val feedImages: MutableList<FeedImage> = mutableListOf()

    val images: List<FeedImage>
        get() = feedImages.toList()

    fun addImage(
        fileName: String,
        sequence: Int,
    ) {
        feedImages += FeedImage(feed = this, fileName = fileName, sequence = sequence)
    }
}

enum class FeedVisibility {
    PUBLIC,
    FRIENDS,
    PRIVATE,
}

@Entity
@Table(
    name = "feed_images",
    uniqueConstraints = [UniqueConstraint(name = "uk_feed_image_sequence", columnNames = ["feed_id", "image_sequence"])],
)
class FeedImage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, updatable = false)
    val feed: Feed,
    @Column(name = "file_name", nullable = false, length = 1000)
    val fileName: String,
    @Column(name = "image_sequence", nullable = false)
    val sequence: Int,
) : BaseTimeEntity()
