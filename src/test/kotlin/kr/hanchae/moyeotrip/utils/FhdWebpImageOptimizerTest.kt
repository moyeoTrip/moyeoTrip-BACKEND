package kr.hanchae.moyeotrip.utils

import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class FhdWebpImageOptimizerTest {
    private val optimizer = FhdWebpImageOptimizer()

    @Test
    fun `FHD 프로필 이미지는 FHD WebP로 변환한다`() {
        val source = png(1920, 1080)

        val optimized = optimizer.optimizeToFhdWebp(source, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED)
        val image = ImageIO.read(ByteArrayInputStream(optimized))

        assertEquals("RIFF", optimized.copyOfRange(0, 4).decodeToString())
        assertEquals("WEBP", optimized.copyOfRange(8, 12).decodeToString())
        assertNotNull(image)
        assertEquals(1920, image.width)
        assertEquals(1080, image.height)
    }

    @Test
    fun `FHD보다 큰 프로필 이미지는 FHD로 축소한다`() {
        val source = png(3840, 2160)

        val optimized = optimizer.optimizeToFhdWebp(source, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED)
        val image = ImageIO.read(ByteArrayInputStream(optimized))

        assertNotNull(image)
        assertEquals(1920, image.width)
        assertEquals(1080, image.height)
    }

    @Test
    fun `FHD보다 작은 프로필 이미지는 확대하지 않는다`() {
        val source = png(640, 480)

        val optimized = optimizer.optimizeToFhdWebp(source, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED)
        val image = ImageIO.read(ByteArrayInputStream(optimized))

        assertNotNull(image)
        assertEquals(640, image.width)
        assertEquals(480, image.height)
    }

    private fun png(
        width: Int,
        height: Int,
    ): ByteArray =
        ByteArrayOutputStream().use { output ->
            ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", output)
            output.toByteArray()
        }
}
