package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.report.ChatRoomReport
import kr.hanchae.moyeotrip.entity.report.UserReport
import org.springframework.data.jpa.repository.JpaRepository

// BE-12: 같은 신고자가 같은 대상을 두 번 신고하지 못하게 막는다(피드 신고와 같은 규칙).
interface ChatRoomReportRepository : JpaRepository<ChatRoomReport, Long> {
    fun existsByChatRoomIdAndReporterId(
        chatRoomId: Long,
        reporterId: Long,
    ): Boolean
}

interface UserReportRepository : JpaRepository<UserReport, Long> {
    fun existsByReportedUserIdAndReporterId(
        reportedUserId: Long,
        reporterId: Long,
    ): Boolean
}
