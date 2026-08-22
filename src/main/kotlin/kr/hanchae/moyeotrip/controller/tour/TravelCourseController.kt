package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.chat.response.PublicTravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseInformationResponse
import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.RateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.response.CoursePublicationResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
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

@Tag(name = "여행 코스", description = "공개 코스 및 채팅방 여행 코스 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/api/v1/travel-courses")
class TravelCourseController(
    private val chatRoomService: ChatRoomService,
    private val travelCourseService: TravelCourseService,
) {
    @Operation(summary = "여행 코스 목록", description = "tagId를 생략하면 전체 코스를 조회합니다.")
    @GetMapping("/public")
    fun getPublicCourses(
        @RequestParam(required = false) tagId: Long?,
    ): List<TravelCourseInformationResponse> = chatRoomService.getPublicCourses(tagId)

    @Operation(summary = "인기 여행 코스 TOP 3", description = "해당 코스로 만들어진 채팅방 수를 기준으로 집계합니다.")
    @GetMapping("/public/popular")
    fun getPopularPublicCourses(): List<TravelCourseInformationResponse> = chatRoomService.getPopularPublicCourses()

    @Operation(summary = "채팅방 여행 코스 조회", description = "채팅방 참가 여부와 관계없이 조회할 수 있습니다.")
    @GetMapping("/chat-rooms/{roomId}")
    fun getRoomCourse(
        @PathVariable roomId: Long,
    ): TravelCourseDetailResponse = chatRoomService.getRoomCourse(roomId)

    @Operation(summary = "채팅방 커스텀 여행 코스 수정", description = "여행 확정 전까지 채팅방 호스트만 수정할 수 있습니다.")
    @PutMapping("/chat-rooms/{roomId}")
    fun updateRoomCourse(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: UpdateTravelCourseRequest,
    ): TravelCourseInformationResponse = chatRoomService.updateRoomCourse(userId, roomId, request)

    @Operation(summary = "여행 코스 태그 전체 조회")
    @GetMapping("/tags")
    fun getCourseTags(): List<TravelCourseTagResponse> = travelCourseService.getCourseTags()

    @Operation(summary = "완료한 커스텀 여행 코스 공개", description = "마이페이지의 지난 여행에서 받은 courseId로 호출합니다.")
    @PostMapping("/{courseId}/publication")
    fun publishCourse(
        @LoginUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: PublishTravelCourseRequest,
    ): CoursePublicationResponse = travelCourseService.publishCourse(userId, courseId, request)

    @Operation(summary = "여행 코스 상세 조회")
    @GetMapping("/{courseId}")
    fun getCourse(
        @PathVariable courseId: Long,
    ): PublicTravelCourseDetailResponse = chatRoomService.getCourse(courseId)

    @Operation(summary = "완료한 여행 코스 평가", description = "확정된 여행이 끝난 채팅방 참가자만 1~5점으로 평가할 수 있습니다.")
    @PostMapping("/chat-rooms/{roomId}/rating")
    fun rateCourse(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: RateTravelCourseRequest,
    ): ResponseEntity<Void> {
        chatRoomService.rateCourse(userId, roomId, request.score)
        return ResponseEntity.noContent().build()
    }
}
