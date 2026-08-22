package kr.hanchae.moyeotrip.entity.user

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

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = [UniqueConstraint(name = "uk_friendship_users", columnNames = ["first_user_id", "second_user_id"])],
)
class Friendship(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "first_user_id", nullable = false, updatable = false)
    val firstUser: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "second_user_id", nullable = false, updatable = false)
    val secondUser: User,
) : BaseTimeEntity() {
    init {
        require(firstUser.id < secondUser.id) { "친구 관계는 작은 사용자 ID가 먼저 와야 합니다." }
    }

    fun friendOf(userId: Long): User = if (firstUser.id == userId) secondUser else firstUser
}

@Entity
@Table(
    name = "friend_requests",
    uniqueConstraints = [UniqueConstraint(name = "uk_friend_request_users", columnNames = ["requester_id", "receiver_id"])],
)
class FriendRequest(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    val requester: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false, updatable = false)
    val receiver: User,
) : BaseTimeEntity() {
    init {
        require(requester.id != receiver.id) { "자기 자신에게 친구 요청을 보낼 수 없습니다." }
    }
}
