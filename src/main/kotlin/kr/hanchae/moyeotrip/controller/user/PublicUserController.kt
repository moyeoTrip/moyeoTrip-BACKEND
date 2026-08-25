package kr.hanchae.moyeotrip.controller.user

import kr.hanchae.moyeotrip.controller.user.response.PublicProfileResponse
import kr.hanchae.moyeotrip.service.auth.UserService
import kr.hanchae.moyeotrip.service.user.TravelCompanionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class PublicUserController(
    private val userService: UserService,
    private val travelCompanionService: TravelCompanionService,
) : PublicUserAPISpec {
    @GetMapping("/{userId}/profile")
    override fun getPublicProfile(
        @PathVariable userId: Long,
    ): PublicProfileResponse = userService.getPublicProfile(userId)

    @GetMapping("/{userId}/travel-reviews")
    override fun getReceivedTravelReviews(
        @PathVariable userId: Long,
    ) = travelCompanionService.getReceivedTravelReviews(userId)
}
