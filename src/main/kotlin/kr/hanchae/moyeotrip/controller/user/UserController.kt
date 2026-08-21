package kr.hanchae.moyeotrip.controller.user

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.config.security.CustomUserDto
import kr.hanchae.moyeotrip.controller.user.request.ProfileImageSelectionRequest
import kr.hanchae.moyeotrip.controller.user.request.UpdateProfileRequest
import kr.hanchae.moyeotrip.controller.user.response.MyProfileResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidatesResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageGenerationResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageSelectionResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileOptionsResponse
import kr.hanchae.moyeotrip.service.auth.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
) : UserAPISpec {
    @GetMapping("/profile")
    fun getProfile(
        @AuthenticationPrincipal principal: CustomUserDto,
    ): MyProfileResponse = userService.getProfile(principal.username.toLong())

    @PutMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal principal: CustomUserDto,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): MyProfileResponse = userService.updateProfile(principal.username.toLong(), request)

    @GetMapping("/profile/options")
    fun getProfileOptions(): ProfileOptionsResponse = userService.getProfileOptions()

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun withdraw(
        @AuthenticationPrincipal principal: CustomUserDto,
    ) {
        userService.withdraw(principal.username.toLong())
    }

    @PostMapping("/profile-images")
    override fun generateProfileImage(
        @AuthenticationPrincipal principal: CustomUserDto,
    ): ProfileImageGenerationResponse = userService.generateProfileImage(principal.username.toLong())

    @GetMapping("/profile-images")
    override fun getProfileImages(
        @AuthenticationPrincipal principal: CustomUserDto,
    ): ProfileImageCandidatesResponse = userService.getProfileImages(principal.username.toLong())

    @PutMapping("/profile-image")
    override fun selectProfileImage(
        @AuthenticationPrincipal principal: CustomUserDto,
        @Valid @RequestBody request: ProfileImageSelectionRequest,
    ): ProfileImageSelectionResponse = userService.selectProfileImage(principal.username.toLong(), request.profileImageId)
}
