package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.controller.user.request.ReviewTravelCompanionRequest
import kr.hanchae.moyeotrip.controller.user.response.TravelDexCompanionResponse
import kr.hanchae.moyeotrip.controller.user.response.TravelDexMemoryResponse
import kr.hanchae.moyeotrip.controller.user.response.TravelDexResponse
import kr.hanchae.moyeotrip.controller.user.response.TripCompanionResponse
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.user.TravelCompanion
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TravelCompanionRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TravelCompanionService(
    private val companionRepository: TravelCompanionRepository,
    private val participantRepository: ChatRoomParticipantRepository,
    private val roomRepository: ChatRoomRepository,
    private val userRepository: UserRepository,
    private val objectStorageRepository: ObjectStorageRepository,
) {
    @Transactional
    fun collectCompletedTrip(room: ChatRoom) {
        val participants = participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
        participants.forEach { owner ->
            participants.asSequence().filter { it.user.id != owner.user.id }.forEach { companion ->
                if (!companionRepository.existsByOwnerIdAndCompanionIdAndChatRoomId(owner.user.id, companion.user.id, room.id)) {
                    companionRepository.save(TravelCompanion(owner = owner.user, companion = companion.user, chatRoom = room))
                }
            }
        }
    }

    @Transactional
    fun getTripCompanions(
        userId: Long,
        roomId: Long,
    ): List<TripCompanionResponse> {
        val room = requireCompletedParticipant(userId, roomId)
        collectCompletedTrip(room)
        return companionRepository.findAllByOwnerIdAndChatRoomIdOrderByIdAsc(userId, roomId).map { it.toTripResponse() }
    }

    @Transactional
    fun reviewCompanion(
        userId: Long,
        roomId: Long,
        companionId: Long,
        request: ReviewTravelCompanionRequest,
    ): TripCompanionResponse {
        val room = requireCompletedParticipant(userId, roomId)
        if (userId == companionId || !participantRepository.existsByChatRoomIdAndUserId(roomId, companionId)) {
            throw BaseException(ErrorCode.FORBIDDEN)
        }
        collectCompletedTrip(room)
        val record =
            companionRepository.findByOwnerIdAndCompanionIdAndChatRoomId(userId, companionId, roomId)
                ?: throw BaseException(ErrorCode.RESOURCE_NOT_FOUND)
        record.review(
            mannerScore = request.mannerScore,
            oneLineReview = request.oneLineReview?.trim()?.takeIf(String::isNotEmpty),
        )
        companionRepository.flush()
        val companion = userRepository.findByIdForUpdate(companionId) ?: throw UserNotFoundException(companionId)
        companionRepository.averageMannerScoreByCompanionId(companionId)?.let(companion::updateMannerRating)
        return record.toTripResponse()
    }

    @Transactional(readOnly = true)
    fun getMyTravelDex(userId: Long): TravelDexResponse {
        findUser(userId)
        val companions =
            companionRepository
                .findAllByOwnerId(userId)
                .groupBy { it.companion.id }
                .values
                .map { records -> records.toDexResponse() }
                .sortedByDescending { it.latestTripDate }
        return TravelDexResponse(totalCount = companions.size, companions = companions)
    }

    private fun requireCompletedParticipant(
        userId: Long,
        roomId: Long,
    ): ChatRoom {
        val room = roomRepository.findById(roomId).orElseThrow { BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND) }
        if (!participantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw BaseException(ErrorCode.CHAT_ROOM_NOT_PARTICIPANT)
        }
        if (!room.hasCompletedTrip(LocalDate.now())) {
            throw BaseException(ErrorCode.TRIP_NOT_COMPLETED)
        }
        return room
    }

    private fun TravelCompanion.toTripResponse(): TripCompanionResponse {
        val information = checkNotNull(companion.information)
        return TripCompanionResponse(
            companionRecordId = id,
            userId = companion.id,
            nickname = information.nickname,
            profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            mannerRating = companion.mannerRating,
            mannerScore = mannerScore,
            oneLineReview = oneLineReview,
            reviewed = mannerScore != null,
        )
    }

    private fun List<TravelCompanion>.toDexResponse(): TravelDexCompanionResponse {
        val latest = maxBy { it.chatRoom.endDate ?: it.chatRoom.startDate }
        val companion = latest.companion
        val information = checkNotNull(companion.information)
        return TravelDexCompanionResponse(
            userId = companion.id,
            nickname = information.nickname,
            profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            mannerRating = companion.mannerRating,
            tripCount = size,
            latestTripDate = latest.chatRoom.endDate ?: latest.chatRoom.startDate,
            latestTripTitle = latest.chatRoom.roomTitle,
            memories =
                sortedByDescending { it.chatRoom.endDate ?: it.chatRoom.startDate }.map {
                    TravelDexMemoryResponse(
                        chatRoomId = it.chatRoom.id,
                        tripTitle = it.chatRoom.roomTitle,
                        tripDate = it.chatRoom.endDate ?: it.chatRoom.startDate,
                        oneLineReview = it.oneLineReview,
                    )
                },
        )
    }

    private fun findUser(userId: Long): User = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
}
