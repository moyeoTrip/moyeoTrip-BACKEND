package kr.hanchae.moyeotrip.controller.user

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.user.request.ProfileImageSelectionRequest
import kr.hanchae.moyeotrip.controller.user.request.UpdateProfileRequest
import kr.hanchae.moyeotrip.controller.user.request.WithdrawRequest
import kr.hanchae.moyeotrip.controller.user.response.MyProfileResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidatesResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageGenerationResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageSelectionResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileOptionsResponse
import kr.hanchae.moyeotrip.controller.user.response.WithdrawalReasonResponse
import kr.hanchae.moyeotrip.service.auth.UserService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.HttpStatus
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
    override fun getProfile(
        @LoginUserId userId: Long,
    ): MyProfileResponse = userService.getProfile(userId)

    @PutMapping("/profile")
    override fun updateProfile(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): MyProfileResponse = userService.updateProfile(userId, request)

    @GetMapping("/profile/options")
    override fun getProfileOptions(): ProfileOptionsResponse = userService.getProfileOptions()

    // 사유는 **선택**이다. 본문 없이 호출하던 기존 클라이언트가 그대로 동작해야 한다 —
    // 세 앱이 아직 배포 전이라도, 탈퇴가 사유 때문에 막히는 일은 없어야 한다.
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun withdraw(
        @LoginUserId userId: Long,
        @RequestBody(required = false) request: WithdrawRequest?,
    ) {
        userService.withdraw(
            userId = userId,
            reason = request?.reason,
            reasonDetail = request?.reasonDetail,
        )
    }

    @GetMapping("/withdrawal-reasons")
    override fun getWithdrawalReasons(): List<WithdrawalReasonResponse> = userService.getWithdrawalReasons()

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
