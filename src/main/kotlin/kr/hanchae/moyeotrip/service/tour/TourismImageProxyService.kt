package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

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
                    val normalizedContentType = contentType.toSupportedImageContentType()
                    if (response.headers.contentLength > MAXIMUM_IMAGE_BYTES) {
                        throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
                    }
                    val bytes = response.body.readNBytes(MAXIMUM_IMAGE_BYTES + 1)
                    if (bytes.size > MAXIMUM_IMAGE_BYTES) {
                        throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
                    }
                    if (normalizedContentType ==
                        BMP_MEDIA_TYPE
                    ) {
                        convertBmpToWebp(bytes)
                    } else {
                        TourismImageBinary(normalizedContentType, bytes)
                    }
                }
        } catch (exception: BaseException) {
            throw exception
        } catch (exception: RestClientException) {
            throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        } catch (exception: Exception) {
            throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        }
    }

    private fun String.toOfficialVisitKoreaImageUri(): URI {
        val uri = runCatching { URI(trim()) }.getOrElse { throw BaseException(ErrorCode.INVALID_TOURISM_IMAGE_URL) }
        val defaultPort =
            when (uri.scheme?.lowercase()) {
                HTTP_SCHEME -> DEFAULT_HTTP_PORT
                HTTPS_SCHEME -> DEFAULT_HTTPS_PORT
                else -> null
            }
        if (
            uri.userInfo != null ||
            defaultPort == null ||
            uri.port !in setOf(defaultPort, NO_EXPLICIT_PORT) ||
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

    private fun MediaType?.toSupportedImageContentType(): MediaType {
        if (this?.type != IMAGE_TYPE) throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        return when (subtype.lowercase()) {
            in JPEG_SUBTYPES -> MediaType.IMAGE_JPEG
            PNG_SUBTYPE -> MediaType.IMAGE_PNG
            BMP_SUBTYPE, X_MS_BMP_SUBTYPE -> BMP_MEDIA_TYPE
            else -> throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        }
    }

    private fun convertBmpToWebp(source: ByteArray): TourismImageBinary {
        val original = ByteArrayInputStream(source).use(ImageIO::read) ?: throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        if (original.width.toLong() * original.height > MAXIMUM_IMAGE_PIXELS) throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        val scale = minOf(1.0, MAX_WEBP_WIDTH.toDouble() / original.width, MAX_WEBP_HEIGHT.toDouble() / original.height)
        val width = (original.width * scale).toInt().coerceAtLeast(1)
        val height = (original.height * scale).toInt().coerceAtLeast(1)
        val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = resized.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(original, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }
        val writer =
            ImageIO.getImageWritersByMIMEType(WEBP_MEDIA_TYPE.toString()).asSequence().firstOrNull()
                ?: throw BaseException(ErrorCode.TOURISM_IMAGE_FETCH_FAILED)
        val bytes =
            try {
                ByteArrayOutputStream().use { output ->
                    ImageIO.createImageOutputStream(output).use { imageOutput ->
                        writer.output = imageOutput
                        val parameter = writer.defaultWriteParam
                        if (parameter.canWriteCompressed()) {
                            parameter.compressionMode = ImageWriteParam.MODE_EXPLICIT
                            parameter.compressionType =
                                parameter.compressionTypes?.firstOrNull { it.equals(LOSSY_COMPRESSION_TYPE, ignoreCase = true) }
                                    ?: parameter.compressionTypes?.firstOrNull()
                            parameter.compressionQuality = WEBP_COMPRESSION_QUALITY
                        }
                        writer.write(null, IIOImage(resized, null, null), parameter)
                    }
                    output.toByteArray()
                }
            } finally {
                writer.dispose()
            }
        return TourismImageBinary(WEBP_MEDIA_TYPE, bytes)
    }

    companion object {
        private const val HTTP_SCHEME = "http"
        private const val HTTPS_SCHEME = "https"
        private const val DEFAULT_HTTP_PORT = 80
        private const val DEFAULT_HTTPS_PORT = 443
        private const val NO_EXPLICIT_PORT = -1
        private const val VISIT_KOREA_DOMAIN = "visitkorea.or.kr"
        private const val MAXIMUM_IMAGE_BYTES = 20 * 1024 * 1024
        private const val IMAGE_TYPE = "image"
        private const val PNG_SUBTYPE = "png"
        private const val BMP_SUBTYPE = "bmp"
        private const val X_MS_BMP_SUBTYPE = "x-ms-bmp"
        private const val MAXIMUM_IMAGE_PIXELS = 40_000_000L
        private const val MAX_WEBP_WIDTH = 1280
        private const val MAX_WEBP_HEIGHT = 720
        private const val LOSSY_COMPRESSION_TYPE = "Lossy"
        private const val WEBP_COMPRESSION_QUALITY = 0.82f
        private val JPEG_SUBTYPES = setOf("jpeg", "jpg", "pjpeg")
        private val BMP_MEDIA_TYPE = MediaType.parseMediaType("image/bmp")
        private val WEBP_MEDIA_TYPE = MediaType.parseMediaType("image/webp")
    }
}
