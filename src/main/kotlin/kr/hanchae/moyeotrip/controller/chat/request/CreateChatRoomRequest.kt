package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.chat.GenderRestriction
import kr.hanchae.moyeotrip.entity.chat.JoinApprovalMode
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class CreateChatRoomRequest(
    @field:NotBlank @field:Size(max = 100)
    val title: String,
    @field:Size(max = 500)
    val description: String? = null,
    @field:Min(3) @field:Max(12)
    val maxParticipants: Int,
    val tripType: TripType,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val recruitmentDeadlineDate: LocalDate,
    val dayTripStartTime: LocalTime? = null,
    val dayTripEndTime: LocalTime? = null,
    @field:Min(-90)
    @field:Max(90)
    val meetingLatitude: Double? = null,
    @field:Min(-180)
    @field:Max(180)
    val meetingLongitude: Double? = null,
    @field:Size(max = 500)
    val meetingDetails: String? = null,
    val meetingDateTime: LocalDateTime,
    @field:Min(0)
    val participationFee: Long? = null,
    val genderRestriction: GenderRestriction,
    @field:Min(20) @field:Max(100)
    val minimumAge: Int? = null,
    @field:Min(20) @field:Max(100)
    val maximumAge: Int? = null,
    val joinApprovalMode: JoinApprovalMode,
    val courseType: TravelCourseType,
    val courseId: Long? = null,
    @field:Valid
    val customCourse: CreateCustomCourseRequest? = null,
)

data class CreateCustomCourseRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Size(max = 500)
    val description: String? = null,
    @field:Valid
    @field:Size(min = 2)
    val places: List<CustomCoursePlaceRequest>,
    val tagIds: Set<Long> = emptySet(),
)

data class CustomCoursePlaceRequest(
    @field:Min(1)
    val contentId: Long,
    @field:Min(1)
    val dayNumber: Int,
    @field:Min(1)
    val sequence: Int,
    val visitTime: LocalTime,
)
