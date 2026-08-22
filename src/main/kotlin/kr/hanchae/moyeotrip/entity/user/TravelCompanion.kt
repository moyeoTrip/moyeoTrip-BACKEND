package kr.hanchae.moyeotrip.entity.user

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
import kr.hanchae.moyeotrip.entity.chat.ChatRoom

@Entity
@Table(
    name = "travel_companions",
    uniqueConstraints = [UniqueConstraint(name = "uk_travel_companion_trip", columnNames = ["owner_id", "companion_id", "chat_room_id"])],
)
class TravelCompanion(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    val owner: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_id", nullable = false, updatable = false)
    val companion: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    mannerScore: Int? = null,
    oneLineReview: String? = null,
) : BaseTimeEntity() {
    @Column(name = "manner_score")
    var mannerScore: Int? = mannerScore
        protected set

    @Column(name = "one_line_review", length = 40)
    var oneLineReview: String? = oneLineReview
        protected set

    init {
        require(owner.id != companion.id)
        mannerScore?.let { require(it in 1..5) }
        oneLineReview?.let { require(it.length <= 40) }
    }

    fun review(
        mannerScore: Int,
        oneLineReview: String?,
    ) {
        require(mannerScore in 1..5)
        require(oneLineReview == null || oneLineReview.length <= 40)
        this.mannerScore = mannerScore
        this.oneLineReview = oneLineReview
    }
}
