package kr.hanchae.moyeotrip.repository

import jakarta.persistence.EntityManager
import kr.hanchae.moyeotrip.entity.user.WithdrawalReason
import kr.hanchae.moyeotrip.entity.user.WithdrawalReasonRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/**
 * 탈퇴 사유는 **사용자가 지워져도 남아야 한다**(이탈 통계용, 2026-09-14 결정).
 * 그 보증은 「`user_id` 를 두지 않았다」는 설계에 통째로 걸려 있어, 실제 DB 로 확인한다.
 */
class WithdrawalReasonRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var withdrawalReasonRepository: WithdrawalReasonRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `사용자를 완전히 지워도 탈퇴 사유는 남는다`() {
        val user = savedUser()
        withdrawalReasonRepository.saveAndFlush(
            WithdrawalReasonRecord(reason = WithdrawalReason.BAD_EXPERIENCE),
        )
        entityManager.flush()
        entityManager.clear()

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.id)

        val survived =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_withdrawal_reasons WHERE reason_code = ?",
                Int::class.java,
                WithdrawalReason.BAD_EXPERIENCE.name,
            )
        assertEquals(1, survived)
    }

    @Test
    fun `기타 사유의 자유 입력도 함께 남는다`() {
        withdrawalReasonRepository.saveAndFlush(
            WithdrawalReasonRecord(reason = WithdrawalReason.OTHER, detail = "쓸 일이 없어졌어요"),
        )

        val saved = withdrawalReasonRepository.findAll().single { it.reason == WithdrawalReason.OTHER }
        assertEquals("쓸 일이 없어졌어요", saved.detail)
    }
}
