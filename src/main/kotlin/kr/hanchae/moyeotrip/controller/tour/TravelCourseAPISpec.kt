package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.chat.response.PublicTravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseInformationResponse
import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.RateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.response.CoursePublicationResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse
import org.springframework.http.ResponseEntity

@Tag(name = "여행 코스", description = "공개 코스 및 채팅방 여행 코스 API")
interface TravelCourseAPISpec {
    @Operation(summary = "여행 코스 목록", description = "tagId를 생략하면 전체 코스를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공개 여행 코스 목록 조회 성공",
                content = [Content(schema = Schema(implementation = TravelCourseInformationResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "유효하지 않은 코스 태그",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.TRAVEL_COURSE_TAG_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getPublicCourses(tagId: Long?): List<TravelCourseInformationResponse>

    @Operation(summary = "인기 여행 코스 TOP 3", description = "해당 코스로 만들어진 채팅방 수를 기준으로 집계합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "인기 공개 여행 코스 조회 성공",
                content = [Content(schema = Schema(implementation = TravelCourseInformationResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "여행 코스 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.INTERNAL_SERVER_ERROR)],
                    ),
                ],
            ),
        ],
    )
    fun getPopularPublicCourses(): List<TravelCourseInformationResponse>

    @Operation(summary = "채팅방 여행 코스 조회", description = "채팅방 참가 여부와 관계없이 조회할 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 여행 코스 조회 성공",
                content = [Content(schema = Schema(implementation = TravelCourseDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 여행 코스를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getRoomCourse(roomId: Long): TravelCourseDetailResponse

    @Operation(summary = "채팅방 커스텀 여행 코스 수정", description = "여행 확정 전까지 채팅방 호스트만 수정할 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "커스텀 여행 코스 수정 성공",
                content = [Content(schema = Schema(implementation = TravelCourseInformationResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "여행 확정 후이거나 수정할 수 없는 커스텀 여행 코스",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.TRAVEL_COURSE_NOT_EDITABLE)],
                    ),
                ],
            ),
        ],
    )
    fun updateRoomCourse(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: UpdateTravelCourseRequest,
    ): TravelCourseInformationResponse

    @Operation(summary = "여행 코스 태그 전체 조회", description = "커스텀 여행 코스 작성과 공개 코스 탐색에 사용할 태그 목록을 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행 코스 태그 목록 조회 성공",
                content = [Content(schema = Schema(implementation = TravelCourseTagResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "여행 코스 태그 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.INTERNAL_SERVER_ERROR)],
                    ),
                ],
            ),
        ],
    )
    fun getCourseTags(): List<TravelCourseTagResponse>

    @Operation(summary = "완료한 커스텀 여행 코스 공개", description = "마이페이지의 지난 여행에서 받은 courseId로 호출합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "커스텀 여행 코스 공개 성공",
                content = [Content(schema = Schema(implementation = CoursePublicationResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "완료한 커스텀 여행 코스가 아니거나 이미 공개 여부를 결정함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.TRAVEL_COURSE_PUBLICATION_NOT_ALLOWED)],
                    ),
                ],
            ),
        ],
    )
    fun publishCourse(
        @Parameter(hidden = true) userId: Long,
        courseId: Long,
        request: PublishTravelCourseRequest,
    ): CoursePublicationResponse

    @Operation(summary = "여행 코스 상세 조회", description = "공개된 여행 코스의 작성자 표시 여부, 평점, 태그와 방문 장소를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공개 여행 코스 상세 조회 성공",
                content = [Content(schema = Schema(implementation = PublicTravelCourseDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공개 여행 코스를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.TRAVEL_COURSE_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getCourse(courseId: Long): PublicTravelCourseDetailResponse

    @Operation(summary = "완료한 여행 코스 평가", description = "확정된 여행이 끝난 채팅방 참가자만 1~5점으로 평가할 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "여행 코스 평가 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "403",
                description = "완료한 여행 채팅방의 참가자가 아니어서 평가할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.TRAVEL_COURSE_RATING_NOT_ALLOWED)],
                    ),
                ],
            ),
        ],
    )
    fun rateCourse(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: RateTravelCourseRequest,
    ): ResponseEntity<Void>
}

private object TravelCourseSwaggerExamples {
    const val TRAVEL_COURSE_TAG_NOT_FOUND = """{"code":40412,"errorMessage":"여행 코스 태그를 찾을 수 없습니다."}"""
    const val TRAVEL_COURSE_NOT_FOUND = """{"code":40403,"errorMessage":"공개된 여행 코스를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
    const val INTERNAL_SERVER_ERROR = """{"code":50000,"errorMessage":"서버에러입니다."}"""
    const val TRAVEL_COURSE_NOT_EDITABLE = """{"code":40912,"errorMessage":"여행 확정 전의 커스텀 코스만 호스트가 수정할 수 있습니다."}"""
    const val TRAVEL_COURSE_PUBLICATION_NOT_ALLOWED = """{"code":40914,"errorMessage":"공개 여부를 선택할 수 있는 완료 코스가 아닙니다."}"""
    const val TRAVEL_COURSE_RATING_NOT_ALLOWED = """{"code":40006,"errorMessage":"완료한 여행의 참가자만 코스를 평가할 수 있습니다."}"""
}
