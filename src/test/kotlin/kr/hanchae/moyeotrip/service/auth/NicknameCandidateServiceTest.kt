package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.repository.NicknameCandidateRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Duration

class NicknameCandidateServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val nicknameCandidateRepository = mock(NicknameCandidateRepository::class.java)
    private val service = NicknameCandidateService(userRepository, nicknameCandidateRepository)

    @Test
    fun `형용사 동물 4자리 숫자와 색상 설명이 있는 후보 3개를 발급한다`() {
        val response = service.generateCandidates()

        assertEquals(3, response.candidates.size)
        assertEquals(
            3,
            response.candidates
                .map { it.nickname }
                .toSet()
                .size,
        )
        assertEquals(600L, response.expiresInSeconds)
        response.candidates.forEach { candidate ->
            assertTrue(candidate.nickname.matches(Regex("^[가-힣]+ [가-힣]+ [0-9]{4}$")))
            assertEquals("${candidate.adjective} ${candidate.animal}", candidate.nickname.substringBeforeLast(" "))
            assertTrue(candidate.color in NicknameColor.entries)
            assertTrue(candidate.description.isNotBlank())
        }
        verify(nicknameCandidateRepository)
            .save(response.selectionToken, response.candidates.associate { it.nickname to it.color }, Duration.ofMinutes(10))
    }

    @Test
    fun `재생성할 때마다 새로운 선택 토큰으로 후보 3개를 다시 발급한다`() {
        val first = service.generateCandidates()
        val second = service.generateCandidates()

        assertTrue(first.selectionToken != second.selectionToken)
        assertEquals(3, first.candidates.size)
        assertEquals(3, second.candidates.size)
        verify(nicknameCandidateRepository)
            .save(first.selectionToken, first.candidates.associate { it.nickname to it.color }, Duration.ofMinutes(10))
        verify(nicknameCandidateRepository)
            .save(second.selectionToken, second.candidates.associate { it.nickname to it.color }, Duration.ofMinutes(10))
    }
}
