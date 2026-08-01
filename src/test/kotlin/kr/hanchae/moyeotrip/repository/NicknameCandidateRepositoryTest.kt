package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.NicknameColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import java.time.Duration

class NicknameCandidateRepositoryTest {
    private val redissonClient = mock(RedissonClient::class.java)

    @Suppress("UNCHECKED_CAST")
    private val bucket = mock(RBucket::class.java) as RBucket<String>
    private val repository = NicknameCandidateRepository(redissonClient)

    @Test
    fun `닉네임 후보를 지정한 만료 시간으로 저장한다`() {
        `when`(redissonClient.getBucket<String>("MoyeoTrip:nickname-candidates:selection-token")).thenReturn(bucket)

        repository.save(
            "selection-token",
            linkedMapOf("따스한 사슴 0000" to NicknameColor.RED, "빠른 거북이 9999" to NicknameColor.BLUE),
            Duration.ofMinutes(10),
        )

        verify(bucket).set("따스한 사슴 0000\u001ERED\u001F빠른 거북이 9999\u001EBLUE", Duration.ofMinutes(10))
    }

    @Test
    fun `닉네임 후보는 한 번만 소비한다`() {
        `when`(redissonClient.getBucket<String>("MoyeoTrip:nickname-candidates:selection-token")).thenReturn(bucket)
        `when`(bucket.getAndDelete())
            .thenReturn("따스한 사슴 0000\u001ERED\u001F빠른 거북이 9999\u001EBLUE", null)

        assertEquals(
            mapOf("따스한 사슴 0000" to NicknameColor.RED, "빠른 거북이 9999" to NicknameColor.BLUE),
            repository.consume("selection-token"),
        )
        assertNull(repository.consume("selection-token"))
    }
}
