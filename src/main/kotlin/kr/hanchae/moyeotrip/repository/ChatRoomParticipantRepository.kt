package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ChatRoomParticipantRepository :
    JpaRepository<ChatRoomParticipant, Long>,
    ChatRoomParticipantCustomRepository {
    fun countByChatRoomId(chatRoomId: Long): Long

    fun existsByChatRoomIdAndUserId(
        chatRoomId: Long,
        userId: Long,
    ): Boolean

    fun findByChatRoomIdAndUserId(
        chatRoomId: Long,
        userId: Long,
    ): ChatRoomParticipant?

    fun findAllByChatRoomIdOrderByCreatedDateTimeAsc(chatRoomId: Long): List<ChatRoomParticipant>

    fun findAllByUserId(userId: Long): List<ChatRoomParticipant>
}

interface ChatRoomParticipantCustomRepository {
    fun countCompletedTrips(userId: Long): Long

    fun hasCompletedTrip(
        roomId: Long,
        userId: Long,
        today: LocalDate,
    ): Boolean
}

class ChatRoomParticipantCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : ChatRoomParticipantCustomRepository {
    override fun countCompletedTrips(userId: Long): Long = countCompletedTrips(userId, LocalDate.now())

    override fun hasCompletedTrip(
        roomId: Long,
        userId: Long,
        today: LocalDate,
    ): Boolean =
        kotlinJdslJpqlExecutor
            .findAll(limit = 1) {
                val existsRoot = entity(User::class, "existsRoot")
                val participant = entity(ChatRoomParticipant::class, "participant")
                val room = participant.path(ChatRoomParticipant::chatRoom)

                select(
                    caseWhen(
                        exists(
                            select(participant.path(ChatRoomParticipant::id))
                                .from(participant)
                                .whereAnd(
                                    participant.path(ChatRoomParticipant::user).path(User::id).eq(userId),
                                    room.path(ChatRoom::id).eq(roomId),
                                    room.path(ChatRoom::status).eq(ChatRoomStatus.CONFIRMED),
                                    or(
                                        and(
                                            room.path(ChatRoom::endDate).isNull(),
                                            room.path(ChatRoom::startDate).lt(today),
                                        ),
                                        room.path(ChatRoom::endDate).lt(today),
                                    ),
                                ).asSubquery(),
                        ),
                    ).then(true).`else`(false),
                ).from(existsRoot)
            }.firstOrNull() ?: false

    private fun countCompletedTrips(
        userId: Long,
        today: LocalDate,
        roomId: Long? = null,
    ): Long =
        kotlinJdslJpqlExecutor
            .findAll {
                val participant = entity(ChatRoomParticipant::class)
                val room = participant.path(ChatRoomParticipant::chatRoom)

                select(count(participant))
                    .from(participant)
                    .whereAnd(
                        participant.path(ChatRoomParticipant::user).path(User::id).eq(userId),
                        room.path(ChatRoom::status).eq(ChatRoomStatus.CONFIRMED),
                        roomId?.let { room.path(ChatRoom::id).eq(it) },
                        or(
                            and(
                                room.path(ChatRoom::endDate).isNull(),
                                room.path(ChatRoom::startDate).lt(today),
                            ),
                            room.path(ChatRoom::endDate).lt(today),
                        ),
                    )
            }.singleOrNull() ?: 0L
}
