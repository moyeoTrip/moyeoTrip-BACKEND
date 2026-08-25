package kr.hanchae.moyeotrip.service.test

import kr.hanchae.moyeotrip.controller.auth.response.TestAccessTokenResponse
import kr.hanchae.moyeotrip.controller.test.response.TestCompletedChatRoomResponse
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TestSupportService(
    private val userRepository: UserRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val jwtUtil: JwtUtil,
) {
    @Transactional(readOnly = true)
    fun issueAccessToken(userId: Long): TestAccessTokenResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        val nickname = user.information?.nickname ?: "사용자 ${user.id}"
        return TestAccessTokenResponse(
            userId = user.id,
            nickname = nickname,
            accessToken = jwtUtil.generateAccessToken(user.id, nickname),
        )
    }

    @Transactional
    fun completeChatRoom(roomId: Long): TestCompletedChatRoomResponse {
        val room = chatRoomRepository.findById(roomId).orElseThrow { BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND) }
        val completedEndDate = LocalDate.now().minusDays(1)
        val startDate = if (room.endDate == null) completedEndDate else completedEndDate.minusDays(1)
        val recruitmentDeadlineDate = startDate.minusDays(1)
        val endDate = room.endDate?.let { completedEndDate }
        chatRoomRepository.completeForTest(roomId, startDate, recruitmentDeadlineDate, endDate)

        return TestCompletedChatRoomResponse(
            roomId = roomId,
            status = ChatRoomStatus.CONFIRMED,
            startDate = startDate,
            recruitmentDeadlineDate = recruitmentDeadlineDate,
            endDate = endDate,
            completed = true,
        )
    }
}
