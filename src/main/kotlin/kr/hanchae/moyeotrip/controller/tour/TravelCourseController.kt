package kr.hanchae.moyeotrip.controller.tour

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.chat.response.PublicTravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.SearchChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseInformationResponse
import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.RateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.response.CoursePublicationResponse
import kr.hanchae.moyeotrip.controller.tour.response.LikedTravelCourseResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseFavoriteResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.service.search.PopularSearchKeywordService
import kr.hanchae.moyeotrip.service.tour.TravelCourseService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/travel-courses")
class TravelCourseController(
    private val chatRoomService: ChatRoomService,
    private val travelCourseService: TravelCourseService,
    private val popularSearchKeywordService: PopularSearchKeywordService,
) : TravelCourseAPISpec {
    @GetMapping("/public")
    override fun getPublicCourses(
        @RequestParam(required = false) tagId: Long?,
    ): List<TravelCourseInformationResponse> = chatRoomService.getPublicCourses(tagId)

    @GetMapping("/search")
    override fun searchPublicCourses(
        @RequestParam(required = false) keyword: String?,
    ): List<TravelCourseInformationResponse> =
        chatRoomService.searchPublicCourses(keyword).also {
            popularSearchKeywordService.record(keyword)
        }

    @GetMapping("/public/popular")
    override fun getPopularPublicCourses(): List<TravelCourseInformationResponse> = chatRoomService.getPopularPublicCourses()

    @GetMapping("/chat-rooms/{roomId}")
    override fun getRoomCourse(
        @PathVariable roomId: Long,
    ): TravelCourseDetailResponse = chatRoomService.getRoomCourse(roomId)

    @PutMapping("/chat-rooms/{roomId}")
    override fun updateRoomCourse(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: UpdateTravelCourseRequest,
    ): TravelCourseInformationResponse = chatRoomService.updateRoomCourse(userId, roomId, request)

    @GetMapping("/tags")
    override fun getCourseTags(): List<TravelCourseTagResponse> = travelCourseService.getCourseTags()

    @PostMapping("/{courseId}/publication")
    override fun publishCourse(
        @LoginUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: PublishTravelCourseRequest,
    ): CoursePublicationResponse = travelCourseService.publishCourse(userId, courseId, request)

    @GetMapping("/{courseId}")
    override fun getCourse(
        @PathVariable courseId: Long,
    ): PublicTravelCourseDetailResponse = chatRoomService.getCourse(courseId)

    @GetMapping("/{courseId}/chat-rooms")
    override fun getPublicCourseChatRooms(
        @LoginUserId userId: Long,
        @PathVariable courseId: Long,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<SearchChatRoomResponse> = chatRoomService.getPublicCourseChatRooms(userId, courseId, limit)

    @PostMapping("/{courseId}/favorite")
    override fun toggleCourseFavorite(
        @LoginUserId userId: Long,
        @PathVariable courseId: Long,
    ): TravelCourseFavoriteResponse = travelCourseService.toggleFavorite(userId, courseId)

    @GetMapping("/me/favorites")
    override fun getLikedCourses(
        @LoginUserId userId: Long,
    ): List<LikedTravelCourseResponse> = travelCourseService.getLikedCourses(userId)

    @PostMapping("/chat-rooms/{roomId}/rating")
    override fun rateCourse(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: RateTravelCourseRequest,
    ): ResponseEntity<Void> {
        chatRoomService.rateCourse(userId, roomId, request.score)
        return ResponseEntity.noContent().build()
    }
}
