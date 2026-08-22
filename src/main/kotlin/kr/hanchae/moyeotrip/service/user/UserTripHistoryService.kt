package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.user.UserTripHistory
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.UserTripHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserTripHistoryService(
    private val historyRepository: UserTripHistoryRepository,
    private val participantRepository: ChatRoomParticipantRepository,
) {
    @Transactional
    fun archive(room: ChatRoom) {
        val participants = participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
        val existingUserIds = historyRepository.findAllByOriginalRoomId(room.id).map { it.user.id }.toSet()
        val histories =
            participants
                .filterNot { it.user.id in existingUserIds }
                .map { participant ->
                    UserTripHistory(
                        user = participant.user,
                        originalRoomId = room.id,
                        travelCourseId = room.course.id,
                        roomTitle = room.roomTitle,
                        roomDescription = room.description,
                        tripStartDate = room.startDate,
                        tripEndDate = room.endDate ?: room.startDate,
                        host = room.host.id == participant.user.id,
                    )
                }
        historyRepository.saveAll(histories)
    }
}
