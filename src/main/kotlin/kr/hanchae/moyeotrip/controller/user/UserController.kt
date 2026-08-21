package kr.hanchae.moyeotrip.controller.user

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.user.request.ProfileImageSelectionRequest
import kr.hanchae.moyeotrip.controller.user.request.UpdateProfileRequest
import kr.hanchae.moyeotrip.controller.user.response.FollowListResponse
import kr.hanchae.moyeotrip.controller.user.response.FollowResponse
import kr.hanchae.moyeotrip.controller.user.response.MyProfileResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidatesResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageGenerationResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageSelectionResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileOptionsResponse
import kr.hanchae.moyeotrip.service.auth.UserService
import kr.hanchae.moyeotrip.service.user.FollowService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class UserController(
    private val userService: UserService,
    private val followService: FollowService,
) : UserAPISpec {
    @PostMapping("/following/{userId}")
    fun toggleFollow(
        @LoginUserId loginUserId: Long,
        @PathVariable userId: Long,
    ): FollowResponse = followService.toggleFollow(loginUserId, userId)

    @GetMapping("/followers")
    fun getFollowers(
        @LoginUserId userId: Long,
    ): FollowListResponse = followService.getFollowers(userId)

    @GetMapping("/following")
    fun getFollowing(
        @LoginUserId userId: Long,
    ): FollowListResponse = followService.getFollowing(userId)

    @GetMapping("/profile")
    fun getProfile(
        @LoginUserId userId: Long,
    ): MyProfileResponse = userService.getProfile(userId)

    @PutMapping("/profile")
    fun updateProfile(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): MyProfileResponse = userService.updateProfile(userId, request)

    @GetMapping("/profile/options")
    fun getProfileOptions(): ProfileOptionsResponse = userService.getProfileOptions()

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun withdraw(
        @LoginUserId userId: Long,
    ) {
        userService.withdraw(userId)
    }

    @PostMapping("/profile-images")
    override fun generateProfileImage(
        @LoginUserId userId: Long,
    ): ProfileImageGenerationResponse = userService.generateProfileImage(userId)

    @GetMapping("/profile-images")
    override fun getProfileImages(
        @LoginUserId userId: Long,
    ): ProfileImageCandidatesResponse = userService.getProfileImages(userId)

    @PutMapping("/profile-image")
    override fun selectProfileImage(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: ProfileImageSelectionRequest,
    ): ProfileImageSelectionResponse = userService.selectProfileImage(userId, request.profileImageId)
}
