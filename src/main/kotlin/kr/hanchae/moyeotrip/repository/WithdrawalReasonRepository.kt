package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.WithdrawalReasonRecord
import org.springframework.data.jpa.repository.JpaRepository

interface WithdrawalReasonRepository : JpaRepository<WithdrawalReasonRecord, Long>
