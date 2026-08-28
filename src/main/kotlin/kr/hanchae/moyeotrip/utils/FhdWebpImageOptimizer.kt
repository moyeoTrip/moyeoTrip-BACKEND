package kr.hanchae.moyeotrip.utils

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.springframework.stereotype.Component
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Component
class FhdWebpImageOptimizer {
    fun optimizeToFhdWebp(
        source: ByteArray,
        failureErrorCode: ErrorCode,
    ): ByteArray =
        try {
            val original =
                ByteArrayInputStream(source).use(ImageIO::read)
                    ?: throw BaseException(failureErrorCode)
            val (width, height) = resizedDimensions(original.width, original.height)
            val optimized = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = optimized.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
                graphics.drawImage(original, 0, 0, width, height, null)
            } finally {
                graphics.dispose()
            }
            encodeWebp(optimized, failureErrorCode)
        } catch (exception: BaseException) {
            throw exception
        } catch (exception: Exception) {
            throw BaseException(failureErrorCode)
        }

    private fun resizedDimensions(
        originalWidth: Int,
        originalHeight: Int,
    ): Pair<Int, Int> {
        val scale =
            minOf(
                1.0,
                MAX_WIDTH.toDouble() / originalWidth,
                MAX_HEIGHT.toDouble() / originalHeight,
            )
        return (originalWidth * scale).toInt().coerceAtLeast(1) to (originalHeight * scale).toInt().coerceAtLeast(1)
    }

    private fun encodeWebp(
        image: BufferedImage,
        failureErrorCode: ErrorCode,
    ): ByteArray {
        val writer =
            ImageIO.getImageWritersByMIMEType(WEBP_MIME_TYPE).asSequence().firstOrNull()
                ?: throw BaseException(failureErrorCode)
        try {
            return ByteArrayOutputStream().use { output ->
                ImageIO.createImageOutputStream(output).use { imageOutput ->
                    writer.output = imageOutput
                    val parameter = writer.defaultWriteParam
                    if (parameter.canWriteCompressed()) {
                        parameter.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        parameter.compressionType =
                            parameter.compressionTypes
                                ?.firstOrNull { it.equals(LOSSY_COMPRESSION_TYPE, ignoreCase = true) }
                                ?: parameter.compressionTypes?.firstOrNull()
                        parameter.compressionQuality = COMPRESSION_QUALITY
                    }
                    writer.write(null, IIOImage(image, null, null), parameter)
                }
                output.toByteArray()
            }
        } finally {
            writer.dispose()
        }
    }

    companion object {
        private const val MAX_WIDTH = 1920
        private const val MAX_HEIGHT = 1080
        private const val COMPRESSION_QUALITY = 0.82f
        private const val LOSSY_COMPRESSION_TYPE = "Lossy"
        private const val WEBP_MIME_TYPE = "image/webp"
    }
}
