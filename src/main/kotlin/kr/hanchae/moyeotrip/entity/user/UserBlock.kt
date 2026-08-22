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
    name = "user_blocks",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_block_blocker_blocked", columnNames = ["blocker_id", "blocked_id"])],
)
class UserBlock(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false, updatable = false)
    val blocker: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false, updatable = false)
    val blocked: User,
) : BaseTimeEntity() {
    init {
        require(blocker.id != blocked.id)
    }
}
