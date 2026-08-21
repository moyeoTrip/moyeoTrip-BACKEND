package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserFollow
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserFollowRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.Optional

class FollowServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val followRepository = mock(UserFollowRepository::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val service = FollowService(userRepository, followRepository, objectStorageRepository)

    @Test
    fun `다른 사용자를 팔로우한다`() {
        val follower = user(1L, "팔로워")
        val following = user(2L, "여행자")
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(follower))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(following))
        `when`(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(null)
        val captor = ArgumentCaptor.forClass(UserFollow::class.java)

        val response = service.toggleFollow(1L, 2L)

        verify(followRepository).save(captor.capture())
        assertEquals(1L, captor.value.follower.id)
        assertEquals(2L, captor.value.following.id)
        assertEquals(true, response.following)
    }

    @Test
    fun `자기 자신은 팔로우할 수 없다`() {
        val exception = assertThrows(BaseException::class.java) { service.toggleFollow(1L, 1L) }

        assertEquals(ErrorCode.BAD_REQUEST, exception.errorCode)
        verifyNoInteractions(userRepository)
        verifyNoInteractions(followRepository)
    }

    @Test
    fun `팔로우 중인 사용자를 토글하면 관계를 삭제한다`() {
        val follow = UserFollow(id = 3L, follower = user(1L, "팔로워"), following = user(2L, "여행자"))
        `when`(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(follow)

        val response = service.toggleFollow(1L, 2L)

        verify(followRepository).delete(follow)
        assertEquals(false, response.following)
        verifyNoInteractions(userRepository)
    }

    private fun user(
        id: Long,
        nickname: String,
    ) = User(
        id = id,
        userRole = UserRole.ROLE_USER,
        userInformation = UserInformation(nickname, NicknameColor.GREEN, Gender.N),
    )
}
