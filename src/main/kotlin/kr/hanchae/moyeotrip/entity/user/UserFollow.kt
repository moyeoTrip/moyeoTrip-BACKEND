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
    name = "user_follows",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_follow_follower_following", columnNames = ["follower_id", "following_id"])],
)
class UserFollow(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false, updatable = false)
    val follower: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id", nullable = false, updatable = false)
    val following: User,
) : BaseTimeEntity() {
    init {
        require(follower.id != following.id) { "자기 자신을 팔로우할 수 없습니다." }
    }
}
