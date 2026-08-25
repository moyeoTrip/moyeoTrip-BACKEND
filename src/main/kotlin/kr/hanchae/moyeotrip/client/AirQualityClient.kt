package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class AirQuality(
    val stationName: String,
    val pm10: Int?,
    val pm25: Int?,
)

interface AirQualityClient {
    fun getGyeongbukAirQuality(): AirQuality?
}

@Component
class AirKoreaApiClient(
    restClientBuilder: RestClient.Builder,
    private val tourApiProperties: TourApiProperties,
) : AirQualityClient {
    private val restClient = restClientBuilder.clone().build()

    override fun getGyeongbukAirQuality(): AirQuality? {
        val response =
            restClient
                .get()
                .uri(createGyeongbukAirQualityUri())
                .retrieve()
                .body(AirKoreaApiResponse::class.java)
                ?: return null
        if (response.response.header.resultCode != SUCCESS_RESULT_CODE) return null
        val items =
            response.response.body
                ?.items
                .orEmpty()
        val item = items.firstOrNull() ?: return null
        return AirQuality(item.stationName, item.pm10Value.toDustConcentration(), item.pm25Value.toDustConcentration())
    }

    private fun String?.toDustConcentration(): Int? =
        this
            ?.trim()
            ?.takeUnless { it == "-" }
            ?.toIntOrNull()

    private fun createGyeongbukAirQualityUri(): URI =
        URI.create(
            "$AIR_KOREA_API_URL?serviceKey=${encodedApiKey()}" +
                "&returnType=json" +
                "&numOfRows=$NUM_OF_ROWS" +
                "&pageNo=$FIRST_PAGE" +
                "&sidoName=${URLEncoder.encode(GYEONGBUK_SIDO_NAME, StandardCharsets.UTF_8)}" +
                "&ver=1.0",
        )

    private fun encodedApiKey(): String =
        tourApiProperties.tourApiKey
            .trim()
            .also { require(it.isNotEmpty()) { "TOUR_API_KEY가 비어 있습니다." } }
            .let { key -> if ('%' in key) key else URLEncoder.encode(key, StandardCharsets.UTF_8) }

    companion object {
        private const val AIR_KOREA_API_URL =
            "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty"
        private const val FIRST_PAGE = 1
        private const val NUM_OF_ROWS = 1
        private const val GYEONGBUK_SIDO_NAME = "경북"
        private const val SUCCESS_RESULT_CODE = "00"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AirKoreaApiResponse(
    val response: AirKoreaResponse,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AirKoreaResponse(
    val header: AirKoreaHeader,
    val body: AirKoreaBody? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AirKoreaHeader(
    val resultCode: String,
    val resultMsg: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AirKoreaBody(
    val totalCount: Int? = null,
    val items: List<AirKoreaItem> = emptyList(),
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AirKoreaItem(
    val stationName: String,
    val pm10Value: String? = null,
    val pm25Value: String? = null,
)
