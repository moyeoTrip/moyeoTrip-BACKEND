package kr.hanchae.moyeotrip.controller.user

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.user.request.ReviewTravelCompanionRequest
import kr.hanchae.moyeotrip.controller.user.response.TravelDexResponse
import kr.hanchae.moyeotrip.controller.user.response.TripCompanionResponse
import kr.hanchae.moyeotrip.service.user.TravelCompanionService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class TravelCompanionController(
    private val travelCompanionService: TravelCompanionService,
) : TravelCompanionAPISpec {
    @GetMapping("/chat-rooms/{roomId}/companions")
    override fun getTripCompanions(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): List<TripCompanionResponse> = travelCompanionService.getTripCompanions(userId, roomId)

    @PutMapping("/chat-rooms/{roomId}/companions/{companionId}/review")
    override fun reviewCompanion(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable companionId: Long,
        @Valid @RequestBody request: ReviewTravelCompanionRequest,
    ): TripCompanionResponse = travelCompanionService.reviewCompanion(userId, roomId, companionId, request)

    @GetMapping("/users/me/travel-dex")
    override fun getMyTravelDex(
        @LoginUserId userId: Long,
    ): TravelDexResponse = travelCompanionService.getMyTravelDex(userId)
}
