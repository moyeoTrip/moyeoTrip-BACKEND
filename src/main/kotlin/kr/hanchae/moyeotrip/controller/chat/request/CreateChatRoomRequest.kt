package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class CreateChatRoomRequest(
    @field:NotBlank @field:Size(max = 100)
    val title: String,
    @field:Size(max = 500)
    val description: String? = null,
    @field:Min(3) @field:Max(20)
    val maxParticipants: Int,
    val startDate: LocalDate,
    val recruitmentDeadlineDate: LocalDate,
    @field:Min(1) @field:Max(30)
    val tripDays: Int,
    val dayTripStartTime: LocalTime? = null,
    val dayTripEndTime: LocalTime? = null,
    @field:Min(-90)
    @field:Max(90)
    val meetingLatitude: Double,
    @field:Min(-180)
    @field:Max(180)
    val meetingLongitude: Double,
    val meetingDateTime: LocalDateTime,
    @field:Min(0)
    val participationFee: Long? = null,
    val managedCourseId: Long? = null,
    @field:Size(max = 100)
    val customCourseTitle: String,
    @field:Valid
    val customPlaces: List<CustomCoursePlaceRequest>,
)

data class CustomCoursePlaceRequest(
    @field:Min(1)
    val contentId: Long,
    @field:Min(1)
    val sequence: Int,
)
