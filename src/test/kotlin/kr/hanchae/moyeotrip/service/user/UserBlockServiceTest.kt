package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserBlock
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.FriendRequestRepository
import kr.hanchae.moyeotrip.repository.FriendshipRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserBlockRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional

class UserBlockServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val blockRepository = mock(UserBlockRepository::class.java)
    private val friendshipRepository = mock(FriendshipRepository::class.java)
    private val friendRequestRepository = mock(FriendRequestRepository::class.java)
    private val storageRepository = mock(ObjectStorageRepository::class.java)
    private val service =
        UserBlockService(userRepository, blockRepository, friendshipRepository, friendRequestRepository, storageRepository)

    @Test
    fun `사용자를 차단하면 친구 관계와 친구 요청을 삭제한다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)))

        val response = service.block(1L, 2L)

        assertEquals(true, response.blocked)
        verify(blockRepository).save(any())
        verify(friendshipRepository).deleteBetween(1L, 2L)
        verify(friendRequestRepository).deleteBetween(1L, 2L)
    }

    @Test
    fun `자기 자신은 차단할 수 없다`() {
        val exception = assertThrows(BaseException::class.java) { service.block(1L, 1L) }

        assertEquals(ErrorCode.SELF_BLOCK_NOT_ALLOWED, exception.errorCode)
        verifyNoInteractions(userRepository, blockRepository, friendshipRepository, friendRequestRepository)
    }

    @Test
    fun `이미 차단한 사용자는 차단 관계를 중복 저장하지 않는다`() {
        val blocker = user(1L)
        val blocked = user(2L)
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(blocker))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(blocked))
        `when`(blockRepository.findByBlockerIdAndBlockedId(1L, 2L)).thenReturn(UserBlock(blocker = blocker, blocked = blocked))

        service.block(1L, 2L)

        verify(blockRepository, org.mockito.Mockito.never()).save(any())
        verify(friendshipRepository).deleteBetween(1L, 2L)
    }

    @Test
    fun `차단 해제는 존재하는 차단만 삭제한다`() {
        val block = UserBlock(blocker = user(1L), blocked = user(2L))
        `when`(blockRepository.findByBlockerIdAndBlockedId(1L, 2L)).thenReturn(block, null)

        val first = service.unblock(1L, 2L)
        val second = service.unblock(1L, 2L)

        assertEquals(false, first.blocked)
        assertEquals(false, second.blocked)
        verify(blockRepository).delete(block)
    }

    @Test
    fun `차단 목록은 프로필 URL과 차단 시각을 반환한다`() {
        val blocker = user(1L)
        val blocked = user(2L)
        blocked.information?.profileFileName = "profile.webp"
        val blockedAt = LocalDateTime.of(2026, 8, 23, 12, 0)
        val block = UserBlock(blocker = blocker, blocked = blocked).withCreatedAt(blockedAt)
        `when`(blockRepository.findAllByBlockerIdOrderByCreatedDateTimeDesc(1L)).thenReturn(listOf(block))
        `when`(storageRepository.getDownloadUrl("profile.webp")).thenReturn("https://cdn/profile.webp")

        val response = service.getBlockedUsers(1L)

        assertEquals("사용자2", response.single().nickname)
        assertEquals("https://cdn/profile.webp", response.single().profileImageUrl)
        assertEquals(blockedAt, response.single().blockedAt)
    }

    private fun user(id: Long): User =
        User(
            id = id,
            userRole = UserRole.ROLE_USER,
            userInformation = UserInformation("사용자$id", NicknameColor.GREEN, Gender.N),
        )

    private fun <T : BaseTimeEntity> T.withCreatedAt(createdAt: LocalDateTime): T {
        BaseTimeEntity::class.java
            .getDeclaredField("createdDateTime")
            .apply { isAccessible = true }
            .set(this, createdAt)
        return this
    }
}
