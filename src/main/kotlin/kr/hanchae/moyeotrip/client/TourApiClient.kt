package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.annotation.JsonProperty
import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class TourApiClient(
    private val tourApiProperties: TourApiProperties,
) {
    private val restClient: RestClient =
        RestClient
            .builder()
            .baseUrl("https://apis.data.go.kr")
            .requestInterceptor { request, body, execution ->
                logger.info("TourAPI request URI: {}", request.uri.toSanitizedString())
                execution.execute(request, body)
            }.build()

    fun getGyeongsangbukdoLegalDongCodes(
        pageNo: Int,
        numOfRows: Int,
    ): TourLegalDongPage {
        val response =
            restClient
                .get()
                .uri(createLegalDongCodeUri(pageNo, numOfRows))
                .retrieve()
                .body(TourLegalDongApiResponse::class.java)
                ?: throw IllegalStateException("한국관광공사 법정동 코드 응답이 비어 있습니다.")

        check(response.response.header.resultCode == SUCCESS_RESULT_CODE) {
            "한국관광공사 법정동 코드 조회에 실패했습니다: ${response.response.header.resultMsg}"
        }
        val body = response.response.body
        return TourLegalDongPage(
            items = body.items?.item.orEmpty(),
            totalCount = body.totalCount,
        )
    }

    fun getClassificationSystemCodes(
        pageNo: Int,
        numOfRows: Int,
    ): TourClassificationSystemPage {
        val response =
            restClient
                .get()
                .uri(createClassificationSystemCodeUri(pageNo, numOfRows))
                .retrieve()
                .body(TourClassificationSystemApiResponse::class.java)
                ?: throw IllegalStateException("한국관광공사 분류체계 코드 응답이 비어 있습니다.")

        check(response.response.header.resultCode == SUCCESS_RESULT_CODE) {
            "한국관광공사 분류체계 코드 조회에 실패했습니다: ${response.response.header.resultMsg}"
        }
        val body = response.response.body
        return TourClassificationSystemPage(
            items = body.items?.item.orEmpty(),
            totalCount = body.totalCount,
        )
    }

    fun getAreaBasedTourismInformation(
        contentTypeId: Int,
        pageNo: Int,
        numOfRows: Int,
    ): TourAreaBasedPage {
        val uri = createAreaBasedListUri(contentTypeId, pageNo, numOfRows)
        val response =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(TourAreaBasedApiResponse::class.java)
                ?: throw IllegalStateException("한국관광공사 지역기반 관광정보 응답이 비어 있습니다.")
        val tourResponse =
            response.response
                ?: throw IllegalStateException(response.openApiServiceResponse?.errorMessage() ?: "알 수 없는 공공데이터포털 오류입니다.")
        check(tourResponse.header.resultCode == SUCCESS_RESULT_CODE) {
            "한국관광공사 지역기반 관광정보 조회에 실패했습니다: ${tourResponse.header.resultMsg}"
        }
        val body = tourResponse.body
        return TourAreaBasedPage(
            items = body.items?.item.orEmpty(),
            totalCount = body.totalCount,
        )
    }

    private fun createLegalDongCodeUri(
        pageNo: Int,
        numOfRows: Int,
    ): URI =
        URI.create(
            "https://apis.data.go.kr/B551011/KorService2/ldongCode2" +
                "?serviceKey=${encodedApiKey()}" +
                "&MobileOS=$MOBILE_OS" +
                "&MobileApp=$MOBILE_APP" +
                "&_type=$RESPONSE_TYPE" +
                "&lDongRegnCd=$GYEONGSANGBUKDO_REGION_CODE" +
                "&lDongListYn=$LEGAL_DONG_LIST_YES" +
                "&pageNo=$pageNo" +
                "&numOfRows=$numOfRows",
        )

    private fun createClassificationSystemCodeUri(
        pageNo: Int,
        numOfRows: Int,
    ): URI =
        URI.create(
            "https://apis.data.go.kr/B551011/KorService2/lclsSystmCode2" +
                "?serviceKey=${encodedApiKey()}" +
                "&MobileOS=$MOBILE_OS" +
                "&MobileApp=$MOBILE_APP" +
                "&_type=$RESPONSE_TYPE" +
                "&lclsSystmListYn=$CLASSIFICATION_SYSTEM_LIST_YES" +
                "&pageNo=$pageNo" +
                "&numOfRows=$numOfRows",
        )

    private fun createAreaBasedListUri(
        contentTypeId: Int,
        pageNo: Int,
        numOfRows: Int,
    ): URI =
        URI.create(
            "https://apis.data.go.kr/B551011/KorService2/areaBasedList2?" +
                    "numOfRows=$numOfRows" +
                    "&pageNo=$pageNo" +
                "&MobileOS=$MOBILE_OS" +
                "&MobileApp=$MOBILE_APP" +
                "&_type=$RESPONSE_TYPE" +
                    "&arrange=$AREA_BASED_ARRANGE" +
                    "&contentTypeId=$contentTypeId" +
                    "&serviceKey=${encodedApiKey()}" +
                "&lDongRegnCd=$GYEONGSANGBUKDO_REGION_CODE"  ,
        )

    private fun encodedApiKey(): String =
        tourApiProperties.tourApiKey.let { key ->
            if ('%' in key) key else URLEncoder.encode(key, StandardCharsets.UTF_8)
        }

    private fun URI.toSanitizedString(): String = toASCIIString().replace(SERVICE_KEY_PATTERN, "serviceKey=***")

    companion object {
        private val logger = LoggerFactory.getLogger(TourApiClient::class.java)
        private val SERVICE_KEY_PATTERN = Regex("serviceKey=[^&]*")
        private const val MOBILE_OS = "WEB"
        private const val MOBILE_APP = "MoyeoTrip"
        private const val RESPONSE_TYPE = "json"
        private const val GYEONGSANGBUKDO_REGION_CODE = "47"
        private const val LEGAL_DONG_LIST_YES = "Y"
        private const val CLASSIFICATION_SYSTEM_LIST_YES = "Y"
        private const val AREA_BASED_ARRANGE = "C"
        private const val SUCCESS_RESULT_CODE = "0000"
    }
}

data class TourLegalDongApiResponse(
    val response: TourLegalDongResponse,
)

data class TourLegalDongResponse(
    val header: TourApiHeader,
    val body: TourLegalDongBody,
)

data class TourApiHeader(
    val resultCode: String,
    val resultMsg: String,
)

data class TourLegalDongBody(
    val items: TourLegalDongItems? = null,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
)

data class TourLegalDongItems(
    val item: List<TourLegalDongItem> = emptyList(),
)

data class TourLegalDongItem(
    val lDongRegnCd: String,
    val lDongRegnNm: String,
    val lDongSignguCd: String,
    val lDongSignguNm: String,
)

data class TourLegalDongPage(
    val items: List<TourLegalDongItem>,
    val totalCount: Int,
)

data class TourClassificationSystemApiResponse(
    val response: TourClassificationSystemResponse,
)

data class TourClassificationSystemResponse(
    val header: TourApiHeader,
    val body: TourClassificationSystemBody,
)

data class TourClassificationSystemBody(
    val items: TourClassificationSystemItems? = null,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
)

data class TourClassificationSystemItems(
    val item: List<TourClassificationSystemItem> = emptyList(),
)

data class TourClassificationSystemItem(
    val lclsSystm1Cd: String,
    val lclsSystm1Nm: String,
    val lclsSystm2Cd: String,
    val lclsSystm2Nm: String,
    val lclsSystm3Cd: String,
    val lclsSystm3Nm: String,
)

data class TourClassificationSystemPage(
    val items: List<TourClassificationSystemItem>,
    val totalCount: Int,
)

data class TourAreaBasedApiResponse(
    val response: TourAreaBasedResponse? = null,
    @param:JsonProperty("OpenAPI_ServiceResponse")
    val openApiServiceResponse: OpenApiServiceErrorResponse? = null,
)

data class OpenApiServiceErrorResponse(
    val cmmMsgHeader: OpenApiErrorHeader? = null,
) {
    fun errorMessage(): String =
        listOfNotNull(cmmMsgHeader?.errMsg, cmmMsgHeader?.returnAuthMsg)
            .filter(String::isNotBlank)
            .joinToString(": ")
            .ifBlank { "공공데이터포털 요청에 실패했습니다. 오류 코드: ${cmmMsgHeader?.returnReasonCode ?: "UNKNOWN"}" }
}

data class OpenApiErrorHeader(
    val errMsg: String? = null,
    val returnAuthMsg: String? = null,
    val returnReasonCode: String? = null,
)

data class TourAreaBasedResponse(
    val header: TourApiHeader,
    val body: TourAreaBasedBody,
)

data class TourAreaBasedBody(
    val items: TourAreaBasedItems? = null,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
)

data class TourAreaBasedItems(
    val item: List<TourAreaBasedItem> = emptyList(),
)

data class TourAreaBasedItem(
    val contentid: String,
    val contenttypeid: String,
    val title: String,
    val addr1: String = "",
    val addr2: String = "",
    val zipcode: String = "",
    val tel: String = "",
    val firstimage: String = "",
    val firstimage2: String = "",
    val cpyrhtDivCd: String = "",
    val mapx: String = "",
    val mapy: String = "",
    val mlevel: String = "",
    val createdtime: String = "",
    val modifiedtime: String = "",
    val lDongRegnCd: String = "",
    val lDongSignguCd: String = "",
    val lclsSystm1: String = "",
    val lclsSystm2: String = "",
    val lclsSystm3: String = "",
)

data class TourAreaBasedPage(
    val items: List<TourAreaBasedItem>,
    val totalCount: Int,
)
