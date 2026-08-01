package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.entity.user.NicknameColor
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfileImagePromptFactoryTest {
    private val factory = ProfileImagePromptFactory()

    @Test
    fun `닉네임의 형용사 동물 색상을 영어 이미지 프롬프트로 변환한다`() {
        val prompt = factory.create("따스한 사슴 2347", NicknameColor.BLUE)

        assertTrue(prompt.contains("warm-hearted"))
        assertTrue(prompt.contains("deer"))
        assertTrue(prompt.contains("blue"))
        assertTrue(prompt.contains("square 1:1"))
        assertTrue(prompt.contains("waist-up portrait"))
        assertFalse(prompt.contains("full-body"))
        assertTrue(prompt.contains("Do not include any text"))
        assertFalse(prompt.contains("2347"))
        assertFalse(prompt.contains("따스한"))
        assertFalse(prompt.contains("사슴"))
    }

    @Test
    fun `색상 enum 이름의 언더스코어를 공백으로 변환한다`() {
        val prompt = factory.create("따스한 사슴 2347", NicknameColor.SKY_BLUE)

        assertTrue(prompt.contains("Use sky blue as the dominant color palette"))
    }

    @Test
    fun `동물 enum 이름의 언더스코어를 공백으로 변환한다`() {
        val prompt = factory.create("따스한 해달 2347", NicknameColor.BLUE)

        assertTrue(prompt.contains("anthropomorphic sea otter"))
    }
}
