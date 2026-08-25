package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.entity.user.FriendRequest
import kr.hanchae.moyeotrip.entity.user.Friendship
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
import kr.hanchae.moyeotrip.service.notification.NotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional

class FriendServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val friendshipRepository = mock(FriendshipRepository::class.java)
    private val requestRepository = mock(FriendRequestRepository::class.java)
    private val blockRepository = mock(UserBlockRepository::class.java)
    private val storageRepository = mock(ObjectStorageRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val service =
        FriendService(
            userRepository,
            friendshipRepository,
            requestRepository,
            blockRepository,
            storageRepository,
            notificationService,
        )

    @Test
    fun `친구 요청을 보내고 상대방에게 알림을 보낸다`() {
        val requester = user(1L)
        val receiver = user(2L)
        val saved = mock(FriendRequest::class.java)
        `when`(saved.id).thenReturn(3L)
        `when`(saved.requester).thenReturn(requester)
        `when`(saved.receiver).thenReturn(receiver)
        `when`(saved.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(requester))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(receiver))
        `when`(requestRepository.save(any(FriendRequest::class.java))).thenReturn(saved)

        val response = service.sendRequest(1L, 2L)

        assertEquals(3L, response.requestId)
        assertEquals(2L, response.user.userId)
        verify(notificationService).notifyFriendRequested(saved)
    }

    @Test
    fun `받은 요청을 수락하면 정렬된 친구 관계를 만든다`() {
        val requester = user(5L)
        val receiver = user(2L)
        val request = FriendRequest(id = 10L, requester = requester, receiver = receiver)
        val friendship = mock(Friendship::class.java)
        `when`(friendship.id).thenReturn(11L)
        `when`(friendship.friendOf(2L)).thenReturn(requester)
        `when`(friendship.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(requestRepository.findByIdAndReceiverId(10L, 2L)).thenReturn(request)
        `when`(friendshipRepository.save(any(Friendship::class.java))).thenReturn(friendship)

        val response = service.acceptRequest(2L, 10L)

        val captor = ArgumentCaptor.forClass(Friendship::class.java)
        verify(friendshipRepository).save(captor.capture())
        assertEquals(2L, captor.value.firstUser.id)
        assertEquals(5L, captor.value.secondUser.id)
        assertEquals(5L, response.user.userId)
        verify(requestRepository).deleteBetween(5L, 2L)
        verify(notificationService).notifyFriendAccepted(friendship, receiver)
    }

    @Test
    fun `친구 요청을 거절하면 요청을 삭제해 다시 신청할 수 있다`() {
        val request = FriendRequest(id = 3L, requester = user(1L), receiver = user(2L))
        `when`(requestRepository.findByIdAndReceiverId(3L, 2L)).thenReturn(request)

        service.rejectRequest(2L, 3L)

        verify(requestRepository).delete(request)
    }

    @Test
    fun `차단 관계에는 친구 요청을 보낼 수 없다`() {
        `when`(blockRepository.existsBetween(1L, 2L)).thenReturn(true)

        val exception = assertThrows(BaseException::class.java) { service.sendRequest(1L, 2L) }

        assertEquals(ErrorCode.USER_BLOCK_RELATIONSHIP, exception.errorCode)
        verifyNoInteractions(userRepository, friendshipRepository, requestRepository, notificationService)
    }

    @Test
    fun `이미 친구이거나 상대가 먼저 신청한 경우 친구 요청을 보낼 수 없다`() {
        `when`(friendshipRepository.existsBetween(1L, 2L)).thenReturn(true)
        val friendException = assertThrows(BaseException::class.java) { service.sendRequest(1L, 2L) }
        assertEquals(ErrorCode.ALREADY_FRIEND, friendException.errorCode)

        `when`(friendshipRepository.existsBetween(1L, 2L)).thenReturn(false)
        `when`(requestRepository.findByRequesterIdAndReceiverId(2L, 1L))
            .thenReturn(FriendRequest(requester = user(2L), receiver = user(1L)))
        val reverseException = assertThrows(BaseException::class.java) { service.sendRequest(1L, 2L) }
        assertEquals(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS, reverseException.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `이미 보낸 친구 요청이 있으면 새로 저장하지 않고 기존 요청을 반환한다`() {
        val existing = mock(FriendRequest::class.java)
        `when`(existing.id).thenReturn(7L)
        `when`(existing.receiver).thenReturn(user(2L))
        `when`(existing.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(requestRepository.findByRequesterIdAndReceiverId(1L, 2L)).thenReturn(existing)

        val response = service.sendRequest(1L, 2L)

        assertEquals(7L, response.requestId)
        verify(requestRepository, org.mockito.Mockito.never()).save(any(FriendRequest::class.java))
    }

    @Test
    fun `받은 친구 요청 목록을 조회한다`() {
        val requester = user(1L)
        val request = mock(FriendRequest::class.java)
        `when`(request.id).thenReturn(3L)
        `when`(request.requester).thenReturn(requester)
        `when`(request.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(requestRepository.findAllByReceiverIdOrderByCreatedDateTimeDesc(2L)).thenReturn(listOf(request))

        val response = service.getReceivedRequests(2L)

        assertEquals(1, response.totalCount)
        assertEquals(
            1L,
            response.requests
                .single()
                .user.userId,
        )
    }

    @Test
    fun `보낸 친구 요청 목록은 수신자 정보로 반환한다`() {
        val request = mock(FriendRequest::class.java)
        `when`(request.id).thenReturn(3L)
        `when`(request.receiver).thenReturn(user(2L))
        `when`(request.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(requestRepository.findAllByRequesterIdOrderByCreatedDateTimeDesc(1L)).thenReturn(listOf(request))

        val response = service.getSentRequests(1L)

        assertEquals(
            2L,
            response.requests
                .single()
                .user.userId,
        )
    }

    @Test
    fun `보낸 친구 요청은 신청자 본인만 취소할 수 있다`() {
        val request = FriendRequest(id = 3L, requester = user(1L), receiver = user(2L))
        `when`(requestRepository.findByIdAndRequesterId(3L, 1L)).thenReturn(request)

        service.cancelRequest(1L, 3L)
        val exception = assertThrows(BaseException::class.java) { service.cancelRequest(2L, 3L) }

        verify(requestRepository).delete(request)
        assertEquals(ErrorCode.FRIEND_REQUEST_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `친구 삭제는 존재하지 않는 관계면 실패한다`() {
        `when`(friendshipRepository.deleteBetween(1L, 2L)).thenReturn(1, 0)

        service.deleteFriend(1L, 2L)
        val exception = assertThrows(BaseException::class.java) { service.deleteFriend(1L, 2L) }

        assertEquals(ErrorCode.FRIENDSHIP_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `친구 목록은 친구가 마지막으로 로그인한 상대 시간을 반환한다`() {
        val friend = user(1L).also { it.recordLogin(LocalDateTime.now().minusHours(1)) }
        val friendship = mock(Friendship::class.java)
        `when`(friendship.id).thenReturn(3L)
        `when`(friendship.friendOf(2L)).thenReturn(friend)
        `when`(friendshipRepository.findAllByUserId(2L)).thenReturn(listOf(friendship))

        val response = service.getFriends(2L)

        assertEquals("1시간 전", response.friends.single().lastActive)
    }

    @Test
    fun `친구의 마지막 접속이 하루 이상 전이면 일 단위로 반환한다`() {
        val friend = user(1L).also { it.recordLogin(LocalDateTime.now().minusDays(3)) }
        val friendship = mock(Friendship::class.java)
        `when`(friendship.friendOf(2L)).thenReturn(friend)
        `when`(friendshipRepository.findAllByUserId(2L)).thenReturn(listOf(friendship))

        val response = service.getFriends(2L)

        assertEquals("3일 전", response.friends.single().lastActive)
    }

    private fun user(id: Long): User =
        User(
            id = id,
            userRole = UserRole.ROLE_USER,
            userInformation = UserInformation("사용자$id", NicknameColor.GREEN, Gender.N),
        )
}
