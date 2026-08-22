package kr.hanchae.moyeotrip.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ProfileImageOptimizerTest {
    private val optimizer = ProfileImageOptimizer()

    @Test
    fun `FHD 프로필 이미지는 HD 이하 WebP로 변환한다`() {
        val source = png(1920, 1080)

        val optimized = optimizer.optimizeToHdWebp(source)
        val image = ImageIO.read(ByteArrayInputStream(optimized))

        assertEquals("RIFF", optimized.copyOfRange(0, 4).decodeToString())
        assertEquals("WEBP", optimized.copyOfRange(8, 12).decodeToString())
        assertNotNull(image)
        assertEquals(1280, image.width)
        assertEquals(720, image.height)
    }

    @Test
    fun `HD보다 작은 프로필 이미지는 확대하지 않는다`() {
        val source = png(640, 480)

        val optimized = optimizer.optimizeToHdWebp(source)
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
