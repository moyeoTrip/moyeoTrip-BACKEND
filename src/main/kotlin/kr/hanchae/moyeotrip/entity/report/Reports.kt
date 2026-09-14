package kr.hanchae.moyeotrip.entity.report

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
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.user.User

// BE-12: 신고 API 가 피드에만 있어 모집·멤버 신고 진입점이 막혀 있었다.
// 구조는 feed_reports 를 그대로 따른다 — 신고자당 대상 1건, 사유 enum + 자유 입력 상세.
@Entity
@Table(
    name = "chat_room_reports",
    uniqueConstraints = [UniqueConstraint(name = "uk_chat_room_report_reporter", columnNames = ["chat_room_id", "reporter_id"])],
)
class ChatRoomReport(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, updatable = false)
    val chatRoom: ChatRoom,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    val reporter: User,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val reason: ChatRoomReportReason,
    @Column(length = 300)
    val details: String? = null,
) : BaseTimeEntity()

// 사람이 아니라 모집글을 신고하는 것이라 「괴롭힘」 대신 모집 내용에 관한 사유를 둔다.
@Schema(
    description =
        "모집 신고 사유. SPAM=스팸 또는 광고, INAPPROPRIATE_CONTENT=부적절한 모집 내용, " +
            "MONEY_TRANSACTION_SOLICITATION=돈거래 유도, FALSE_INFORMATION=허위 정보, " +
            "COMMERCIAL_PURPOSE=영리 목적 모집, OTHER=기타",
    allowableValues = [
        "SPAM",
        "INAPPROPRIATE_CONTENT",
        "MONEY_TRANSACTION_SOLICITATION",
        "FALSE_INFORMATION",
        "COMMERCIAL_PURPOSE",
        "OTHER",
    ],
)
enum class ChatRoomReportReason(
    val displayName: String,
) {
    SPAM("스팸 또는 광고"),
    INAPPROPRIATE_CONTENT("부적절한 모집 내용"),
    MONEY_TRANSACTION_SOLICITATION("돈거래 유도"),
    FALSE_INFORMATION("허위 정보"),
    COMMERCIAL_PURPOSE("영리 목적 모집"),
    OTHER("기타"),
}

// 같은 사람을 방마다 다시 신고하게 두면 중복 판정이 어려워, 신고자·대상 한 쌍에 1건만 남긴다.
// 어느 방에서 신고했는지는 운영이 확인할 수 있도록 함께 저장한다.
@Entity
@Table(
    name = "user_reports",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_report_reporter", columnNames = ["reported_user_id", "reporter_id"])],
)
class UserReport(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_user_id", nullable = false, updatable = false)
    val reportedUser: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
    val reporter: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", updatable = false)
    val chatRoom: ChatRoom? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val reason: UserReportReason,
    @Column(length = 300)
    val details: String? = null,
) : BaseTimeEntity()

// 사람 신고라 「허위 정보」 대신 괴롭힘·사칭·약속 불이행이 들어간다.
@Schema(
    description =
        "멤버 신고 사유. HARASSMENT=괴롭힘 또는 혐오 표현, MONEY_TRANSACTION_SOLICITATION=돈거래 유도, " +
            "UNFAIR_SETTLEMENT=부당한 정산 요구, " +
            "INAPPROPRIATE_BEHAVIOR=부적절한 프로필 또는 대화, IMPERSONATION=사칭, " +
            "NO_SHOW=약속 불이행 또는 노쇼, SPAM=스팸 또는 광고, OTHER=기타",
    allowableValues = [
        "HARASSMENT",
        "MONEY_TRANSACTION_SOLICITATION",
        "UNFAIR_SETTLEMENT",
        "INAPPROPRIATE_BEHAVIOR",
        "IMPERSONATION",
        "NO_SHOW",
        "SPAM",
        "OTHER",
    ],
)
enum class UserReportReason(
    val displayName: String,
) {
    HARASSMENT("괴롭힘 또는 혐오 표현"),
    MONEY_TRANSACTION_SOLICITATION("돈거래 유도"),

    /**
     * 정산은 서비스가 아니라 사람끼리 직접 하는 일이라 금액을 서버가 검증할 수 없다.
     * 대신 「금액이 부당하면 신고하세요」 안내를 정산 화면에 두기로 했고(2026-09-14 결정),
     * 그 안내가 「기타」로 떨어지지 않도록 사유를 따로 둔다.
     */
    UNFAIR_SETTLEMENT("부당한 정산 요구"),
    INAPPROPRIATE_BEHAVIOR("부적절한 프로필 또는 대화"),
    IMPERSONATION("사칭"),
    NO_SHOW("약속 불이행 또는 노쇼"),
    SPAM("스팸 또는 광고"),
    OTHER("기타"),
}
