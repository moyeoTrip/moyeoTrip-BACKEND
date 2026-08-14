package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
    private val objectMapper: ObjectMapper,
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
        val errorMessage = response.openApiServiceResponse?.errorMessage() ?: "알 수 없는 공공데이터포털 오류입니다."
        val tourResponse = response.response ?: throw IllegalStateException(errorMessage)
        check(tourResponse.header.resultCode == SUCCESS_RESULT_CODE) {
            "한국관광공사 지역기반 관광정보 조회에 실패했습니다: ${tourResponse.header.resultMsg}"
        }
        val body = tourResponse.body
        return TourAreaBasedPage(
            items = body.items?.item.orEmpty(),
            totalCount = body.totalCount,
        )
    }

    fun getCommonDetail(contentId: Long): TourCommonDetailItem? {
        val uri = createCommonDetailUri(contentId)
        val response =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(TourCommonDetailApiResponse::class.java)
                ?: throw IllegalStateException("한국관광공사 상세정보 응답이 비어 있습니다.")

        val tourResponse =
            response.response
                ?: throw IllegalStateException(response.openApiServiceResponse?.errorMessage() ?: "알 수 없는 공공데이터포털 오류입니다.")
        check(tourResponse.header.resultCode == SUCCESS_RESULT_CODE) {
            "한국관광공사 상세정보 조회에 실패했습니다: ${tourResponse.header.resultMsg}"
        }
        return tourResponse.body.items
            ?.item
            ?.firstOrNull()
    }

    fun getIntroDetail(
        contentId: Long,
        contentTypeId: Int,
    ): JsonNode = getDynamicDetail(createIntroDetailUri(contentId, contentTypeId), "소개정보")

    fun getAdditionalDetails(
        contentId: Long,
        contentTypeId: Int,
    ): JsonNode = getDynamicDetail(createAdditionalDetailUri(contentId, contentTypeId), "반복정보")

    fun getImages(
        contentId: Long,
        imageYn: String,
    ): JsonNode = getDynamicDetail(createImageDetailUri(contentId, imageYn), "이미지정보($imageYn)")

    private fun getDynamicDetail(
        uri: URI,
        apiName: String,
    ): JsonNode {
        val root =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(JsonNode::class.java)
                ?: throw IllegalStateException("한국관광공사 $apiName 응답이 비어 있습니다.")
        val response = root.path("response")
        check(!response.isMissingNode) { "한국관광공사 $apiName 조회에 실패했습니다." }
        val header = response.path("header")
        check(header.path("resultCode").asText() == SUCCESS_RESULT_CODE) {
            "한국관광공사 $apiName 조회에 실패했습니다: ${header.path("resultMsg").asText()}"
        }
        val item = response.path("body").path("items").path("item")
        return when {
            item.isArray -> item
            item.isObject -> objectMapper.createArrayNode().add(item)
            else -> objectMapper.createArrayNode()
        }
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
                "&lDongRegnCd=$GYEONGSANGBUKDO_REGION_CODE",
        )

    private fun createCommonDetailUri(contentId: Long): URI =
        URI.create(
            "https://apis.data.go.kr/B551011/KorService2/detailCommon2" +
                "?serviceKey=${encodedApiKey()}" +
                "&MobileOS=$MOBILE_OS" +
                "&MobileApp=$MOBILE_APP" +
                "&_type=$RESPONSE_TYPE" +
                "&contentId=$contentId" +
                "&pageNo=1" +
                "&numOfRows=1",
        )

    private fun createIntroDetailUri(
        contentId: Long,
        contentTypeId: Int,
    ): URI = createContentTypeDetailUri("detailIntro2", contentId, contentTypeId, 1)

    private fun createAdditionalDetailUri(
        contentId: Long,
        contentTypeId: Int,
    ): URI = createContentTypeDetailUri("detailInfo2", contentId, contentTypeId, DETAIL_NUM_OF_ROWS)

    private fun createContentTypeDetailUri(
        endpoint: String,
        contentId: Long,
        contentTypeId: Int,
        numOfRows: Int,
    ): URI =
        URI.create(
            "https://apis.data.go.kr/B551011/KorService2/$endpoint" +
                "?serviceKey=${encodedApiKey()}" +
                "&MobileOS=$MOBILE_OS" +
                "&MobileApp=$MOBILE_APP" +
                "&_type=$RESPONSE_TYPE" +
                "&contentId=$contentId" +
                "&contentTypeId=$contentTypeId" +
                "&pageNo=1" +
                "&numOfRows=$numOfRows",
        )

    private fun createImageDetailUri(
        contentId: Long,
        imageYn: String,
    ): URI =
        URI.create(
            "https://apis.data.go.kr/B551011/KorService2/detailImage2" +
                "?serviceKey=${encodedApiKey()}" +
                "&MobileOS=$MOBILE_OS" +
                "&MobileApp=$MOBILE_APP" +
                "&_type=$RESPONSE_TYPE" +
                "&contentId=$contentId" +
                "&imageYN=$imageYn" +
                "&pageNo=1" +
                "&numOfRows=$DETAIL_NUM_OF_ROWS",
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
        private const val DETAIL_NUM_OF_ROWS = 1000
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

data class TourCommonDetailApiResponse(
    val response: TourCommonDetailResponse? = null,
    @param:JsonProperty("OpenAPI_ServiceResponse")
    val openApiServiceResponse: OpenApiServiceErrorResponse? = null,
)

data class TourCommonDetailResponse(
    val header: TourApiHeader,
    val body: TourCommonDetailBody,
)

data class TourCommonDetailBody(
    val items: TourCommonDetailItems? = null,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
)

data class TourCommonDetailItems(
    val item: List<TourCommonDetailItem> = emptyList(),
)

data class TourCommonDetailItem(
    val contentid: String,
    val contenttypeid: String = "",
    val title: String = "",
    val createdtime: String = "",
    val modifiedtime: String = "",
    val tel: String = "",
    val telname: String = "",
    val homepage: String = "",
    val booktour: String = "",
    val firstimage: String = "",
    val firstimage2: String = "",
    val cpyrhtDivCd: String = "",
    val addr1: String = "",
    val addr2: String = "",
    val zipcode: String = "",
    val mapx: String = "",
    val mapy: String = "",
    val mlevel: String = "",
    val lDongRegnCd: String = "",
    val lDongSignguCd: String = "",
    val overview: String = "",
)
