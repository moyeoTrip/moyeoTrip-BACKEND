package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.chat.response.PublicTravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseInformationResponse
import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.RateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.response.CoursePublicationResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import org.springframework.http.ResponseEntity

@Tag(name = "여행 코스", description = "공개 코스 및 채팅방 여행 코스 API")
@SecurityRequirement(name = "Authorization")
interface TravelCourseAPISpec {
    @Operation(summary = "여행 코스 목록", description = "tagId를 생략하면 전체 코스를 조회합니다.")
    fun getPublicCourses(tagId: Long?): List<TravelCourseInformationResponse>

    @Operation(summary = "인기 여행 코스 TOP 3", description = "해당 코스로 만들어진 채팅방 수를 기준으로 집계합니다.")
    fun getPopularPublicCourses(): List<TravelCourseInformationResponse>

    @Operation(summary = "채팅방 여행 코스 조회", description = "채팅방 참가 여부와 관계없이 조회할 수 있습니다.")
    fun getRoomCourse(roomId: Long): TravelCourseDetailResponse

    @Operation(summary = "채팅방 커스텀 여행 코스 수정", description = "여행 확정 전까지 채팅방 호스트만 수정할 수 있습니다.")
    fun updateRoomCourse(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: UpdateTravelCourseRequest,
    ): TravelCourseInformationResponse

    @Operation(summary = "여행 코스 태그 전체 조회", description = "커스텀 여행 코스 작성과 공개 코스 탐색에 사용할 태그 목록을 반환합니다.")
    fun getCourseTags(): List<TravelCourseTagResponse>

    @Operation(summary = "완료한 커스텀 여행 코스 공개", description = "마이페이지의 지난 여행에서 받은 courseId로 호출합니다.")
    fun publishCourse(
        @Parameter(hidden = true) userId: Long,
        courseId: Long,
        request: PublishTravelCourseRequest,
    ): CoursePublicationResponse

    @Operation(summary = "여행 코스 상세 조회", description = "공개된 여행 코스의 작성자 표시 여부, 평점, 태그와 방문 장소를 반환합니다.")
    fun getCourse(courseId: Long): PublicTravelCourseDetailResponse

    @Operation(summary = "완료한 여행 코스 평가", description = "확정된 여행이 끝난 채팅방 참가자만 1~5점으로 평가할 수 있습니다.")
    fun rateCourse(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: RateTravelCourseRequest,
    ): ResponseEntity<Void>
}
