package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
                content = [Content(array = ArraySchema(schema = Schema(implementation = TravelCourseInformationResponse::class)))],
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
    fun getPublicCourses(
        @Parameter(description = "필터링할 여행 코스 태그 ID. 생략하면 전체 공개 코스를 반환합니다.", example = "1")
        tagId: Long?,
    ): List<TravelCourseInformationResponse>

    @Operation(summary = "공개 여행 코스 검색", description = "검색어가 코스 제목에 포함되거나 코스 태그명과 일치하는 공개 코스를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공개 여행 코스 검색 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = TravelCourseInformationResponse::class)))],
            ),
        ],
    )
    fun searchPublicCourses(
        @Parameter(description = "코스 제목 포함 또는 태그명 일치 검색어. 예: 바다", example = "바다") keyword: String?,
    ): List<TravelCourseInformationResponse>

    @Operation(summary = "인기 여행 코스 TOP 3", description = "해당 코스로 만들어진 채팅방 수를 기준으로 집계합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "인기 공개 여행 코스 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = TravelCourseInformationResponse::class)))],
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
                description = "채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getRoomCourse(
        @Parameter(description = "여행 코스를 조회할 채팅방 ID", example = "101")
        roomId: Long,
    ): TravelCourseDetailResponse

    @Operation(summary = "채팅방 커스텀 여행 코스 수정", description = "여행 확정 전까지 채팅방 호스트만 수정할 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "커스텀 여행 코스 수정 성공",
                content = [Content(schema = Schema(implementation = TravelCourseInformationResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 본문이 유효하지 않거나 여행 일차별 코스 구성이 올바르지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 본문 검증 실패", value = TravelCourseSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "일차별 장소·순서 구성 오류", value = TravelCourseSwaggerExamples.INVALID_TRAVEL_COURSE_SCHEDULE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 수정할 관광 장소를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = TravelCourseSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "관광 장소 없음", value = TravelCourseSwaggerExamples.TOURISM_CONTENT_NOT_FOUND),
                        ],
                    ),
                ],
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
        @Parameter(description = "커스텀 여행 코스를 수정할 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "커스텀 코스 제목, 소개, 방문 장소와 태그", required = true)
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
            ApiResponse(
                responseCode = "403",
                description = "완료한 커스텀 여행 코스를 공개할 호스트가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.TRAVEL_COURSE_OWNER_REQUIRED)],
                    ),
                ],
            ),
        ],
    )
    fun publishCourse(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "공개 여부를 결정할 완료 커스텀 여행 코스 ID", example = "77")
        courseId: Long,
        @Parameter(description = "공개 제목, 소개 및 작성자 닉네임 표시 여부", required = true)
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
    fun getCourse(
        @Parameter(description = "상세 조회할 공개 여행 코스 ID", example = "77")
        courseId: Long,
    ): PublicTravelCourseDetailResponse

    @Operation(
        summary = "공개 코스로 만든 모집 중 채팅방 목록",
        description = "해당 공개 코스로 만들어진 채팅방 중 모집 마감 전이고 로그인 사용자가 아직 참가하지 않은 방을 반환합니다. 차단 관계가 있는 호스트 또는 참가자가 있는 방은 제외합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공개 코스로 만든 모집 중 채팅방 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = SearchChatRoomResponse::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
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
    fun getPublicCourseChatRooms(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "채팅방을 조회할 공개 여행 코스 ID", example = "77")
        courseId: Long,
        @Parameter(description = "반환할 최대 개수. 1~20이며 기본값은 20", example = "20")
        limit: Int,
    ): List<SearchChatRoomResponse>

    @Operation(summary = "공개 여행 코스 찜 토글", description = "호출할 때마다 로그인 사용자의 코스 찜 상태를 반전합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행 코스 찜 상태 변경 성공",
                content = [Content(schema = Schema(implementation = TravelCourseFavoriteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공개 여행 코스 또는 로그인 사용자를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun toggleCourseFavorite(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "찜 상태를 바꿀 공개 여행 코스 ID", example = "77")
        courseId: Long,
    ): TravelCourseFavoriteResponse

    @Operation(summary = "내가 찜한 여행 코스 목록", description = "로그인 사용자가 찜한 공개 여행 코스를 가장 최근에 찜한 순서로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "찜한 여행 코스 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = LikedTravelCourseResponse::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCourseSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getLikedCourses(
        @Parameter(hidden = true) userId: Long,
    ): List<LikedTravelCourseResponse>

    @Operation(summary = "완료한 여행 코스 평가", description = "확정된 여행이 끝난 채팅방 참가자만 1~5점으로 평가할 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "여행 코스 평가 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 로그인 사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = TravelCourseSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "로그인 사용자 없음", value = TravelCourseSwaggerExamples.USER_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
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
        @Parameter(description = "평가할 완료 여행의 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "1~5점 코스 평점", required = true)
        request: RateTravelCourseRequest,
    ): ResponseEntity<Void>
}

private object TravelCourseSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val INVALID_TRAVEL_COURSE_SCHEDULE = """{"code":40007,"errorMessage":"여행 일차마다 방문지를 최소 2개 편성해야 합니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val TRAVEL_COURSE_TAG_NOT_FOUND = """{"code":40412,"errorMessage":"여행 코스 태그를 찾을 수 없습니다."}"""
    const val TRAVEL_COURSE_NOT_FOUND = """{"code":40403,"errorMessage":"공개된 여행 코스를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
    const val TOURISM_CONTENT_NOT_FOUND = """{"code":40408,"errorMessage":"관광 콘텐츠를 찾을 수 없습니다."}"""
    const val INTERNAL_SERVER_ERROR = """{"code":50000,"errorMessage":"서버에러입니다."}"""
    const val TRAVEL_COURSE_NOT_EDITABLE = """{"code":40912,"errorMessage":"여행 확정 전의 커스텀 코스만 호스트가 수정할 수 있습니다."}"""
    const val TRAVEL_COURSE_PUBLICATION_NOT_ALLOWED = """{"code":40914,"errorMessage":"공개 여부를 선택할 수 있는 완료 코스가 아닙니다."}"""
    const val TRAVEL_COURSE_RATING_NOT_ALLOWED = """{"code":40006,"errorMessage":"완료한 여행의 참가자만 코스를 평가할 수 있습니다."}"""
    const val TRAVEL_COURSE_OWNER_REQUIRED = """{"code":40303,"errorMessage":"해당 여행 코스를 공개할 호스트가 아닙니다."}"""
}
