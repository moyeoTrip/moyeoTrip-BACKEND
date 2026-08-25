package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomFavorite
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomFavoriteRepository :
    JpaRepository<ChatRoomFavorite, Long>,
    ChatRoomFavoriteCustomRepository {
    fun existsByUserIdAndChatRoomId(
        userId: Long,
        chatRoomId: Long,
    ): Boolean

    fun findByUserIdAndChatRoomId(
        userId: Long,
        chatRoomId: Long,
    ): ChatRoomFavorite?
}

interface ChatRoomFavoriteCustomRepository {
    fun findChatRoomsByUserIdOrderByFavoritedAtDesc(userId: Long): List<ChatRoom>

    fun findChatRoomIdsByUserIdAndChatRoomIdIn(
        userId: Long,
        chatRoomIds: Collection<Long>,
    ): Set<Long>
}

class ChatRoomFavoriteCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : ChatRoomFavoriteCustomRepository {
    override fun findChatRoomsByUserIdOrderByFavoritedAtDesc(userId: Long): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val favorite = entity(ChatRoomFavorite::class)
                val room = entity(ChatRoom::class)

                select(room)
                    .from(
                        favorite,
                        innerJoin(favorite.path(ChatRoomFavorite::chatRoom)).`as`(room),
                    ).where(favorite.path(ChatRoomFavorite::user).path(User::id).eq(userId))
                    .orderBy(
                        favorite.path(ChatRoomFavorite::createdDateTime).desc(),
                        favorite.path(ChatRoomFavorite::id).desc(),
                    )
            }.filterNotNull()

    override fun findChatRoomIdsByUserIdAndChatRoomIdIn(
        userId: Long,
        chatRoomIds: Collection<Long>,
    ): Set<Long> {
        if (chatRoomIds.isEmpty()) return emptySet()

        return kotlinJdslJpqlExecutor
            .findAll {
                val favorite = entity(ChatRoomFavorite::class)
                val chatRoomId = favorite.path(ChatRoomFavorite::chatRoom).path(ChatRoom::id)

                select(chatRoomId)
                    .from(favorite)
                    .whereAnd(
                        favorite.path(ChatRoomFavorite::user).path(User::id).eq(userId),
                        chatRoomId.`in`(chatRoomIds),
                    )
            }.filterNotNull()
            .toSet()
    }
}
