package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class TourApiClient
    @Autowired
    constructor(
        private val objectMapper: ObjectMapper,
        private val tourApiProperties: TourApiProperties,
    ) {
        internal constructor(
            restClientBuilder: RestClient.Builder,
            objectMapper: ObjectMapper,
            tourApiProperties: TourApiProperties,
        ) : this(objectMapper, tourApiProperties) {
            restClient = createRestClient(restClientBuilder)
        }

        private var restClient: RestClient = createRestClient(RestClient.builder())

        private fun createRestClient(restClientBuilder: RestClient.Builder): RestClient =
            restClientBuilder
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
            val rawResponse = getCommonDetailRawJson(contentId)
            check(rawResponse.statusCode in 200..299) {
                "한국관광공사 상세정보 HTTP 요청에 실패했습니다: status=${rawResponse.statusCode}, body=${rawResponse.body}"
            }
            val response = objectMapper.readValue(rawResponse.body, TourCommonDetailApiResponse::class.java)

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

        fun getCommonDetailRawJson(contentId: Long): TourApiRawJsonResponse {
            val uri = createCommonDetailUri(contentId)
            logCommonDetailRequestDiagnostics(contentId, uri)
            return restClient
                .get()
                .uri(uri)
                .exchange { _, response ->
                    TourApiRawJsonResponse(
                        statusCode = response.statusCode.value(),
                        body = response.body.bufferedReader(StandardCharsets.UTF_8).use { it.readText() },
                    )
                }.also { rawResponse ->
                    if (rawResponse.statusCode !in 200..299) {
                        logger.warn(
                            "TourAPI detailCommon2 failed. contentId={}, status={}, body={}",
                            contentId,
                            rawResponse.statusCode,
                            rawResponse.body,
                        )
                    }
                }
        }

        private fun logCommonDetailRequestDiagnostics(
            contentId: Long,
            uri: URI,
        ) {
            val serviceKeyQueryValue =
                uri.rawQuery
                    .split('&')
                    .firstOrNull { it.startsWith("serviceKey=") }
                    ?.substringAfter('=')
                    .orEmpty()
            logger.info(
                "TourAPI detailCommon2 request diagnostics. contentId={}, rawPlus={}, doubleEncoded={}, encodedPadding={}, keyLength={}",
                contentId,
                '+' in serviceKeyQueryValue,
                "%25" in serviceKeyQueryValue,
                "%3D" in serviceKeyQueryValue.uppercase(),
                serviceKeyQueryValue.length,
            )
        }

        fun getImages(
            contentId: Long,
            imageYn: String,
        ): List<TourImageItem> {
            val response =
                restClient
                    .get()
                    .uri(createImageDetailUri(contentId, imageYn))
                    .retrieve()
                    .body(TourImageApiResponse::class.java)
                    ?: throw IllegalStateException("한국관광공사 이미지정보 응답이 비어 있습니다.")

            val tourResponse =
                response.response
                    ?: throw IllegalStateException(response.openApiServiceResponse?.errorMessage() ?: "알 수 없는 공공데이터포털 오류입니다.")
            check(tourResponse.header.resultCode == SUCCESS_RESULT_CODE) {
                "한국관광공사 이미지정보 조회에 실패했습니다: ${tourResponse.header.resultMsg}"
            }
            return tourResponse.body.items
                ?.item
                .orEmpty()
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
                    "&numOfRows=$IMAGE_NUM_OF_ROWS",
            )

        private fun encodedApiKey(): String =
            tourApiProperties.tourApiKey
                .trim()
                .also {
                    require(it.isNotEmpty()) { "TOUR_API_KEY가 비어 있습니다." }
                }.let { key ->
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
            private const val IMAGE_NUM_OF_ROWS = 1000
        }
    }

data class TourApiRawJsonResponse(
    val statusCode: Int,
    val body: String,
)

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

@JsonIgnoreProperties(ignoreUnknown = true)
data class TourCommonDetailApiResponse(
    val response: TourCommonDetailResponse? = null,
    @param:JsonProperty("OpenAPI_ServiceResponse")
    val openApiServiceResponse: OpenApiServiceErrorResponse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TourCommonDetailResponse(
    val header: TourApiHeader,
    val body: TourCommonDetailBody,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TourCommonDetailBody(
    val items: TourCommonDetailItems? = null,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TourCommonDetailItems(
    val item: List<TourCommonDetailItem> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TourCommonDetailItem(
    val contentid: String,
    val contenttypeid: String = "",
    val title: String = "",
    val createdtime: String = "",
    val modifiedtime: String = "",
    val tel: String = "",
    val telname: String = "",
    val homepage: String = "",
    val firstimage: String = "",
    val firstimage2: String = "",
    val cpyrhtDivCd: String = "",
    val areacode: String = "",
    val sigungucode: String = "",
    val addr1: String = "",
    val addr2: String = "",
    val zipcode: String = "",
    val mapx: String = "",
    val mapy: String = "",
    val mlevel: String = "",
    val lDongRegnCd: String = "",
    val lDongSignguCd: String = "",
    val lclsSystm1: String = "",
    val lclsSystm2: String = "",
    val lclsSystm3: String = "",
    val cat1: String = "",
    val cat2: String = "",
    val cat3: String = "",
    val overview: String = "",
)

data class TourImageApiResponse(
    val response: TourImageResponse? = null,
    @param:JsonProperty("OpenAPI_ServiceResponse")
    val openApiServiceResponse: OpenApiServiceErrorResponse? = null,
)

data class TourImageResponse(
    val header: TourApiHeader,
    val body: TourImageBody,
)

data class TourImageBody(
    val items: TourImageItems? = null,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
)

data class TourImageItems(
    val item: List<TourImageItem> = emptyList(),
)

data class TourImageItem(
    val contentid: String,
    val imgname: String = "",
    val originimgurl: String = "",
    val serialnum: String = "",
    val smallimageurl: String = "",
    val cpyrhtDivCd: String = "",
)
