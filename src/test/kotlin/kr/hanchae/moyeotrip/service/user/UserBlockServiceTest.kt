package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
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

        assertEquals(ErrorCode.BAD_REQUEST, exception.errorCode)
        verifyNoInteractions(userRepository, blockRepository, friendshipRepository, friendRequestRepository)
    }

    private fun user(id: Long): User =
        User(
            id = id,
            userRole = UserRole.ROLE_USER,
            userInformation = UserInformation("사용자$id", NicknameColor.GREEN, Gender.N),
        )
}
