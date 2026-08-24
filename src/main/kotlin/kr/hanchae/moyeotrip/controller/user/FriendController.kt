package kr.hanchae.moyeotrip.controller.user

import kr.hanchae.moyeotrip.controller.user.response.FriendListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendResponse
import kr.hanchae.moyeotrip.service.user.FriendService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class FriendController(
    private val friendService: FriendService,
) : FriendAPISpec {
    @PostMapping("/friend-requests/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    override fun sendRequest(
        @LoginUserId loginUserId: Long,
        @PathVariable userId: Long,
    ): FriendRequestResponse = friendService.sendRequest(loginUserId, userId)

    @PostMapping("/friend-requests/{requestId}/accept")
    override fun acceptRequest(
        @LoginUserId userId: Long,
        @PathVariable requestId: Long,
    ): FriendResponse = friendService.acceptRequest(userId, requestId)

    @PostMapping("/friend-requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun rejectRequest(
        @LoginUserId userId: Long,
        @PathVariable requestId: Long,
    ) = friendService.rejectRequest(userId, requestId)

    @DeleteMapping("/friend-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun cancelRequest(
        @LoginUserId userId: Long,
        @PathVariable requestId: Long,
    ) = friendService.cancelRequest(userId, requestId)

    @GetMapping("/friend-requests/received")
    override fun getReceivedRequests(
        @LoginUserId userId: Long,
    ): FriendRequestListResponse = friendService.getReceivedRequests(userId)

    @GetMapping("/friend-requests/sent")
    override fun getSentRequests(
        @LoginUserId userId: Long,
    ): FriendRequestListResponse = friendService.getSentRequests(userId)

    @GetMapping("/friends")
    override fun getFriends(
        @LoginUserId userId: Long,
    ): FriendListResponse = friendService.getFriends(userId)

    @DeleteMapping("/friends/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteFriend(
        @LoginUserId userId: Long,
        @PathVariable friendUserId: Long,
    ) = friendService.deleteFriend(userId, friendUserId)
}
