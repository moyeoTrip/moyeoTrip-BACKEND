package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kr.hanchae.moyeotrip.config.properties.WeatherApiProperties
import org.apache.http.client.utils.URIBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class KmaWeatherGrid(
    val x: Int,
    val y: Int,
)

data class KmaWeatherForecast(
    val forecastAt: LocalDateTime,
    val temperatureCelsius: Double?,
    val humidityPercent: Int?,
    val windSpeedMetersPerSecond: Double?,
    val precipitationMillimeters: Double?,
    val skyCode: Int?,
    val precipitationTypeCode: Int?,
)

interface KmaWeatherClient {
    fun getUltraShortForecast(
        baseDateTime: LocalDateTime,
        grid: KmaWeatherGrid,
    ): KmaWeatherForecast?
}

@Component
class KmaWeatherApiClient(
    restClientBuilder: RestClient.Builder,
    private val properties: WeatherApiProperties,
) : KmaWeatherClient {
    private val restClient = restClientBuilder.clone().baseUrl(properties.baseUrl).build()

    override fun getUltraShortForecast(
        baseDateTime: LocalDateTime,
        grid: KmaWeatherGrid,
    ): KmaWeatherForecast? {
        check(properties.serviceKey.isNotBlank()) { "기상청 API 인증 키가 설정되지 않았습니다." }
        val uri =
            URIBuilder()
                .setScheme(API_SCHEME)
                .setHost(API_HOST)
                .setPath(ULTRA_SHORT_FORECAST_PATH)
                .addParameter("authKey", properties.serviceKey.trim())
                .addParameter("pageNo", "1")
                .addParameter("numOfRows", FORECAST_CATEGORY_COUNT.toString())
                .addParameter("dataType", "JSON")
                .addParameter("base_date", baseDateTime.format(DATE_FORMAT))
                .addParameter("base_time", baseDateTime.format(TIME_FORMAT))
                .addParameter("nx", grid.x.toString())
                .addParameter("ny", grid.y.toString())
                .build()
        val response =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body<KmaUltraShortForecastApiResponse>()
                ?: return null

        if (response.response.header.resultCode != SUCCESS_RESULT_CODE) return null
        val points =
            response.response.body?.items?.item.orEmpty().mapNotNull { item ->
                item.forecastDateTimeOrNull()?.let { dateTime -> KmaForecastPoint(item.category, dateTime, item.fcstValue) }
            }
        val target = points.filter { !it.forecastAt.isBefore(baseDateTime) }.minByOrNull { it.forecastAt }?.forecastAt ?: return null
        val values = points.filter { it.forecastAt == target }.associate { it.category to it.value }
        return KmaWeatherForecast(
            forecastAt = target,
            temperatureCelsius = values["T1H"].toWeatherDouble(),
            humidityPercent = values["REH"].toWeatherDouble()?.toInt(),
            windSpeedMetersPerSecond = values["WSD"].toWeatherDouble(),
            precipitationMillimeters = values["RN1"].toPrecipitationMillimeters(),
            skyCode = values["SKY"].toWeatherDouble()?.toInt(),
            precipitationTypeCode = values["PTY"].toWeatherDouble()?.toInt(),
        )
    }

    private fun KmaUltraShortForecastItem.forecastDateTimeOrNull(): LocalDateTime? =
        runCatching { LocalDateTime.parse("$fcstDate$fcstTime", DATE_TIME_FORMAT) }.getOrNull()

    private fun String?.toWeatherDouble(): Double? =
        this
            ?.trim()
            ?.replace("mm", "")
            ?.replace("m/s", "")
            ?.toDoubleOrNull()

    private fun String?.toPrecipitationMillimeters(): Double? {
        val value = this?.trim() ?: return null
        return when {
            value == "강수없음" -> 0.0
            value.contains("1mm 미만") -> 0.5
            else -> value.toWeatherDouble()
        }
    }

    private data class KmaForecastPoint(
        val category: String,
        val forecastAt: LocalDateTime,
        val value: String,
    )

    companion object {
        private const val API_SCHEME = "https"
        private const val API_HOST = "apihub.kma.go.kr"
        private const val ULTRA_SHORT_FORECAST_PATH = "/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtFcst"
        private const val FORECAST_CATEGORY_COUNT = 11
        private const val SUCCESS_RESULT_CODE = "00"
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
        private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KmaUltraShortForecastApiResponse(
    val response: KmaUltraShortForecastResponse,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KmaUltraShortForecastResponse(
    val header: KmaUltraShortForecastHeader,
    val body: KmaUltraShortForecastBody? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KmaUltraShortForecastHeader(
    val resultCode: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KmaUltraShortForecastBody(
    val items: KmaUltraShortForecastItems? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KmaUltraShortForecastItems(
    val item: List<KmaUltraShortForecastItem>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KmaUltraShortForecastItem(
    val category: String,
    val fcstDate: String,
    val fcstTime: String,
    val fcstValue: String,
)
