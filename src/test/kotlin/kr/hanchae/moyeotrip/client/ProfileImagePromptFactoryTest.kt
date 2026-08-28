package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.entity.user.NicknameColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfileImagePromptFactoryTest {
    private val factory = ProfileImagePromptFactory()

    @Test
    fun `닉네임의 형용사 동물 색상을 레퍼런스 기반 영어 이미지 프롬프트로 변환한다`() {
        val prompt = factory.create("따스한 사슴 2347", NicknameColor.BLUE, userId = 7L, generationNumber = 1)

        assertTrue(prompt.contains("warm-hearted"))
        assertTrue(prompt.contains("deer"))
        assertTrue(prompt.contains("clear medium blue"))
        assertTrue(prompt.contains("#367FB5"))
        assertTrue(prompt.contains("Square 1:1 canvas"))
        assertTrue(prompt.contains("attached image only as the canonical reference"))
        assertTrue(prompt.contains("one complete compact body"))
        assertTrue(prompt.contains("Do not include text"))
        assertFalse(prompt.contains("2347"))
        assertFalse(prompt.contains("따스한"))
        assertFalse(prompt.contains("사슴"))
    }

    @Test
    fun `모든 닉네임 색상은 고유한 정확한 팔레트를 가진다`() {
        val prompts =
            NicknameColor.entries.map { color ->
                factory.create("따스한 사슴 2347", color, userId = 7L, generationNumber = 1)
            }

        assertEquals(NicknameColor.entries.size, prompts.map(::extractExactPalette).toSet().size)
        assertTrue(prompts.all { it.contains("must cover at least 60%") })
    }

    @Test
    fun `동물 enum 이름의 언더스코어를 공백으로 변환한다`() {
        val prompt = factory.create("따스한 해달 2347", NicknameColor.BLUE, userId = 7L, generationNumber = 1)

        assertTrue(prompt.contains("anthropomorphic sea otter"))
    }

    @Test
    fun `같은 사용자의 세 후보는 서로 다른 배경 장면을 사용한다`() {
        val scenes =
            (1..3).map { generationNumber ->
                factory
                    .create("따스한 사슴 2347", NicknameColor.RED, userId = 7L, generationNumber = generationNumber)
                    .lineSequence()
                    .first { it.startsWith("- Scene:") }
            }

        assertEquals(3, scenes.toSet().size)
        assertNotEquals(scenes[0], scenes[1])
    }

    private fun extractExactPalette(prompt: String): String = prompt.lineSequence().first { it.startsWith("- Exact palette:") }
}
