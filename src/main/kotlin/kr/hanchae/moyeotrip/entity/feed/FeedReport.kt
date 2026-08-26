package kr.hanchae.moyeotrip.entity.feed

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
    name = "feed_reports",
    uniqueConstraints = [UniqueConstraint(name = "uk_feed_report_reporter", columnNames = ["feed_id", "reporter_id"])],
)
class FeedReport(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, updatable = false)
    val feed: Feed,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    val reporter: User,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val reason: FeedReportReason,
    @Column(length = 300)
    val details: String? = null,
) : BaseTimeEntity()

enum class FeedReportReason(
    val displayName: String,
) {
    SPAM("스팸 또는 광고"),
    INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
    HARASSMENT("괴롭힘 또는 혐오 표현"),
    FALSE_INFORMATION("허위 정보"),
    OTHER("기타"),
}
