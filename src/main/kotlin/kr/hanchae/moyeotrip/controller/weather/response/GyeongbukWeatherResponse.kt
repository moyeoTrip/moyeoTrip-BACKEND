package kr.hanchae.moyeotrip.controller.weather.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "경상북도 현재 시각 기준 날씨 정보")
data class GyeongbukWeatherResponse(
    @field:Schema(
        description =
            "여행 화면에 표시할 대표 날씨 상태. SUNNY=맑음, CLOUDY=구름/흐림, RAIN=비, SNOW=눈, FOG=안개, " +
                "STRONG_WIND=강풍, HEAVY_RAIN=폭우, HEAT_WAVE=폭염, FINE_DUST=미세먼지 나쁨",
        example = "RAIN",
    )
    val condition: GyeongbukWeatherCondition,
    @field:Schema(description = "날씨 데이터를 조회한 지역명", example = "경상북도 포항시")
    val locationName: String,
    @field:Schema(description = "경북 기본 격자 조회 실패로 안동 대표 격자를 사용했는지 여부", example = "false")
    val fallbackApplied: Boolean,
    @field:Schema(description = "기상청 예보의 적용 시각", example = "2026-08-25T14:00:00")
    val forecastAt: LocalDateTime,
    @field:Schema(description = "기온(℃)", nullable = true, example = "27.5")
    val temperatureCelsius: Double?,
    @field:Schema(description = "상대 습도(%)", nullable = true, example = "82")
    val humidityPercent: Int?,
    @field:Schema(description = "풍속(m/s)", nullable = true, example = "4.2")
    val windSpeedMetersPerSecond: Double?,
    @field:Schema(description = "1시간 강수량(mm). 강수 없음은 0", nullable = true, example = "3.5")
    val precipitationMillimeters: Double?,
    @field:Schema(description = "PM10 농도(㎍/㎥). AirKorea 데이터가 없으면 null", nullable = true, example = "54")
    val pm10: Int?,
    @field:Schema(description = "PM2.5 농도(㎍/㎥). AirKorea 데이터가 없으면 null", nullable = true, example = "18")
    val pm25: Int?,
)

@Schema(
    description =
        "대표 날씨 상태. SUNNY=맑음, CLOUDY=구름 많음 또는 흐림, RAIN=비, SNOW=눈, FOG=안개, STRONG_WIND=강풍, " +
            "HEAVY_RAIN=폭우, HEAT_WAVE=폭염, FINE_DUST=미세먼지 나쁨입니다. " +
            "위험 상태 우선순위는 HEAVY_RAIN > STRONG_WIND > HEAT_WAVE > SNOW > RAIN > FOG > FINE_DUST > CLOUDY > SUNNY입니다. " +
            "HEAVY_RAIN=1시간 강수량 30mm 이상, STRONG_WIND=풍속 14m/s 이상, HEAT_WAVE=기온 33℃ 이상, " +
            "FOG=습도 95% 이상·풍속 1.5m/s 미만·무강수, FINE_DUST=PM10 81 또는 PM2.5 36㎍/㎥ 이상입니다.",
)
enum class GyeongbukWeatherCondition {
    @Schema(description = "맑음: 비·눈·안개·강풍·폭우·폭염·미세먼지·구름 조건에 해당하지 않는 상태")
    SUNNY,

    @Schema(description = "구름/흐림: 기상청 하늘 상태가 구름 많음 또는 흐림인 상태")
    CLOUDY,

    @Schema(description = "비: 비, 빗방울 또는 비와 눈이 섞인 강수 상태")
    RAIN,

    @Schema(description = "눈: 눈 또는 눈날림 강수 상태")
    SNOW,

    @Schema(description = "안개: 습도 95% 이상, 풍속 1.5m/s 미만이며 강수가 없는 상태")
    FOG,

    @Schema(description = "강풍: 풍속이 14m/s 이상인 상태")
    STRONG_WIND,

    @Schema(description = "폭우: 1시간 강수량이 30mm 이상인 상태")
    HEAVY_RAIN,

    @Schema(description = "폭염: 기온이 33℃ 이상인 상태")
    HEAT_WAVE,

    @Schema(description = "미세먼지 나쁨: PM10이 81 또는 PM2.5가 36㎍/㎥ 이상인 상태")
    FINE_DUST,
}
