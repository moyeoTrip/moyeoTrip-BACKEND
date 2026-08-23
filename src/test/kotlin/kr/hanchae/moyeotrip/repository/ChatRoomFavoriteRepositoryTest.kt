package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomFavorite
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChatRoomFavoriteRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var favoriteRepository: ChatRoomFavoriteRepository

    @Test
    fun `사용자가 찜한 채팅방 ID만 요청 범위에서 조회한다`() {
        val user = savedUser()
        val otherUser = savedUser()
        val host = savedUser()
        val course = savedCourse()
        val firstRoom = savedRoom(host, course)
        val secondRoom = savedRoom(host, course)
        val excludedRoom = savedRoom(host, course)
        favoriteRepository.saveAllAndFlush(
            listOf(
                ChatRoomFavorite(user = user, chatRoom = firstRoom),
                ChatRoomFavorite(user = user, chatRoom = secondRoom),
                ChatRoomFavorite(user = otherUser, chatRoom = excludedRoom),
            ),
        )

        val result =
            favoriteRepository.findChatRoomIdsByUserIdAndChatRoomIdIn(
                user.id,
                listOf(firstRoom.id, excludedRoom.id),
            )

        assertEquals(setOf(firstRoom.id), result)
    }

    @Test
    fun `채팅방 ID가 비어 있으면 빈 결과를 반환한다`() {
        assertEquals(emptySet<Long>(), favoriteRepository.findChatRoomIdsByUserIdAndChatRoomIdIn(1L, emptyList()))
    }
}
