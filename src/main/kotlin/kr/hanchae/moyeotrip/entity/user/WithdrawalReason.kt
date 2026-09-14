package kr.hanchae.moyeotrip.entity.user

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseTimeEntity

/**
 * 탈퇴할 때 고른 사유. **누가 골랐는지는 남기지 않는다.**
 *
 * 세 플랫폼 탈퇴 화면이 사유 5종을 고르게 해 놓고 **서버가 받을 곳이 없어 그대로 버리고 있었다**
 * (QA `BE-25`) — 묻고 버리는 것은 사용자를 속이는 셈이라 받기로 했다.
 *
 * **이 기록은 탈퇴 뒤에도 남는다.** 이탈 원인 통계를 뽑는 것이 목적이라, 사용자가 지워져도
 * 함께 지워지면 안 된다(사용자 결정, 2026-09-14).
 *
 * 그래서 **`user_id` 를 두지 않는다.** 둘을 동시에 만족시키는 유일한 방법이다 —
 * 식별자를 달아 두면 「탈퇴 시 지체 없이 파기」(개인정보 처리방침 3항)와 충돌하고,
 * 파기하자니 통계가 사라진다. 사유와 시각만 남기면 **개인과 무관한 집계 자료**가 되어
 * `users` 행이 지워져도(`permanentlyDelete`) 그대로 살아 있다 — FK 가 없어 CASCADE 도 타지 않는다.
 */
@Entity
@Table(name = "user_withdrawal_reasons")
class WithdrawalReasonRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 50, updatable = false)
    val reason: WithdrawalReason,
    /** 「기타」를 고른 경우의 자유 입력. 개인을 식별할 만한 내용이 들어올 수 있어 길이를 제한한다. */
    @Column(name = "reason_detail", length = 200, updatable = false)
    val detail: String? = null,
) : BaseTimeEntity()

@Schema(
    description = "회원 탈퇴 사유",
    allowableValues = [
        "TRAVEL_LESS_OFTEN",
        "NO_APPEALING_RECRUITMENT",
        "BAD_EXPERIENCE",
        "TOO_MANY_NOTIFICATIONS",
        "OTHER",
    ],
)
enum class WithdrawalReason(
    val displayName: String,
) {
    TRAVEL_LESS_OFTEN("여행을 자주 가지 않게 됐어요"),
    NO_APPEALING_RECRUITMENT("마음에 드는 모집이 없어요"),
    BAD_EXPERIENCE("불쾌한 경험이 있었어요"),
    TOO_MANY_NOTIFICATIONS("알림이 너무 많아요"),
    OTHER("기타"),
}
