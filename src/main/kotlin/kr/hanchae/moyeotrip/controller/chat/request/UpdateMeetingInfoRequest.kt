package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UpdateMeetingInfoRequest(
    @field:Min(-90)
    @field:Max(90)
    val meetingLatitude: Double? = null,
    @field:Min(-180)
    @field:Max(180)
    val meetingLongitude: Double? = null,
    @field:Size(max = 500)
    val meetingDetails: String? = null,
    val meetingDateTime: LocalDateTime,
)
