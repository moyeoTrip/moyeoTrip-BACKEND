package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseResponse
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "여행 코스", description = "공개 코스 및 채팅방 여행 코스 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/api/v1/travel-courses")
class TravelCourseController(
    private val chatRoomService: ChatRoomService,
) {
    @Operation(summary = "다른 여행자가 다녀온 공개 여행 코스 목록")
    @GetMapping("/public")
    fun getPublicCourses(): List<TravelCourseResponse> = chatRoomService.getPublicCourses()

    @Operation(summary = "인기 공개 여행 코스 TOP 3", description = "해당 코스로 만들어진 채팅방 수를 기준으로 집계합니다.")
    @GetMapping("/public/popular")
    fun getPopularPublicCourses(): List<TravelCourseResponse> = chatRoomService.getPopularPublicCourses()

    @Operation(summary = "채팅방 여행 코스 조회", description = "해당 채팅방 참가자만 조회할 수 있습니다.")
    @GetMapping("/chat-rooms/{roomId}")
    fun getRoomCourse(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): TravelCourseResponse = chatRoomService.getRoomCourse(userId, roomId)
}
