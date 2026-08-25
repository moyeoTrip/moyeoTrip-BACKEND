package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.URI

data class TourismImageBinary(
    val contentType: MediaType,
    val bytes: ByteArray,
)

@Service
class TourismImageProxyService(
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().build()

    fun getImage(imageUrl: String): TourismImageBinary {
        val uri = imageUrl.toOfficialVisitKoreaImageUri()
        return try {
            restClient
                .get()
                .uri(uri)
                .exchange { _, response ->
                    if (!response.statusCode.is2xxSuccessful) {
                        throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
                    }
                    val contentType = response.headers.contentType
                    if (contentType?.type != MediaType.IMAGE_JPEG.type) {
                        throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
                    }
                    if (response.headers.contentLength > MAXIMUM_IMAGE_BYTES) {
                        throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
                    }
                    val bytes = response.body.readNBytes(MAXIMUM_IMAGE_BYTES + 1)
                    if (bytes.size > MAXIMUM_IMAGE_BYTES) {
                        throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
                    }
                    TourismImageBinary(contentType = contentType, bytes = bytes)
                }
        } catch (exception: BaseException) {
            throw exception
        } catch (exception: RestClientException) {
            throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        }
    }

    private fun String.toOfficialVisitKoreaImageUri(): URI {
        val uri = runCatching { URI(trim()) }.getOrElse { throw BaseException(ErrorCode.INVALID_TOURISM_IMAGE_URL) }
        if (
            uri.scheme != HTTPS_SCHEME ||
            uri.userInfo != null ||
            uri.port !in setOf(DEFAULT_HTTPS_PORT, NO_EXPLICIT_PORT) ||
            !uri.host.isOfficialVisitKoreaHost()
        ) {
            throw BaseException(ErrorCode.INVALID_TOURISM_IMAGE_URL)
        }
        return uri
    }

    private fun String?.isOfficialVisitKoreaHost(): Boolean =
        this
            ?.lowercase()
            ?.let { it == VISIT_KOREA_DOMAIN || it.endsWith(".$VISIT_KOREA_DOMAIN") }
            ?: false

    companion object {
        private const val HTTPS_SCHEME = "https"
        private const val DEFAULT_HTTPS_PORT = 443
        private const val NO_EXPLICIT_PORT = -1
        private const val VISIT_KOREA_DOMAIN = "visitkorea.or.kr"
        private const val MAXIMUM_IMAGE_BYTES = 20 * 1024 * 1024
    }
}
