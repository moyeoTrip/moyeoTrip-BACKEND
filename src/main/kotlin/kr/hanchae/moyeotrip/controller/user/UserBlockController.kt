package kr.hanchae.moyeotrip.controller.user

import kr.hanchae.moyeotrip.controller.user.response.BlockedUserResponse
import kr.hanchae.moyeotrip.controller.user.response.UserBlockResponse
import kr.hanchae.moyeotrip.service.user.UserBlockService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/blocks")
class UserBlockController(
    private val userBlockService: UserBlockService,
) {
    @PostMapping("/{userId}")
    fun block(
        @LoginUserId loginUserId: Long,
        @PathVariable userId: Long,
    ): UserBlockResponse = userBlockService.block(loginUserId, userId)

    @DeleteMapping("/{userId}")
    fun unblock(
        @LoginUserId loginUserId: Long,
        @PathVariable userId: Long,
    ): UserBlockResponse = userBlockService.unblock(loginUserId, userId)

    @GetMapping
    fun getBlockedUsers(
        @LoginUserId userId: Long,
    ): List<BlockedUserResponse> = userBlockService.getBlockedUsers(userId)
}
