package kr.hanchae.moyeotrip.client

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class FirebaseAuthenticationClientTest {
    private val firebaseAuth = mock(FirebaseAuth::class.java)
    private val client = FirebaseAuthenticationClient(firebaseAuth)

    @Test
    fun `Firebase password 로그인은 이메일 제공자로 판별한다`() {
        val token = firebaseToken(provider = "password", uid = "email-uid", email = "user@example.com")
        `when`(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token)

        val identity = client.verifyIdToken("id-token")

        assertEquals(ProviderType.EMAIL, identity.providerType)
        assertEquals("email-uid", identity.uid)
        assertEquals("user@example.com", identity.email)
    }

    @Test
    fun `Firebase Apple 로그인은 Apple 제공자로 판별한다`() {
        val token = firebaseToken(provider = "apple.com", uid = "apple-uid")
        `when`(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token)

        assertEquals(ProviderType.APPLE, client.verifyIdToken("id-token").providerType)
    }

    @Test
    fun `카카오 커스텀 토큰 로그인은 custom claim으로 판별한다`() {
        val token = firebaseToken(provider = "custom", uid = "12345", customProvider = "KAKAO")
        `when`(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token)

        assertEquals(ProviderType.KAKAO, client.verifyIdToken("id-token").providerType)
    }

    @Test
    fun `Firebase Google 로그인은 Google 제공자로 판별한다`() {
        val token = firebaseToken(provider = "google.com", uid = "google-uid")
        `when`(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token)

        assertEquals(ProviderType.GOOGLE, client.verifyIdToken("id-token").providerType)
    }

    @Test
    fun `지원하지 않는 Firebase 제공자는 거부한다`() {
        val token = firebaseToken(provider = "github.com", uid = "github-uid")
        `when`(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token)

        val exception = assertThrows(BaseException::class.java) { client.verifyIdToken("id-token") }

        assertEquals(ErrorCode.INVALID_AUTH_PROVIDER, exception.errorCode)
    }

    private fun firebaseToken(
        provider: String,
        uid: String,
        email: String? = null,
        customProvider: String? = null,
    ): FirebaseToken {
        val token = mock(FirebaseToken::class.java)
        val claims = mutableMapOf<String, Any>("firebase" to mapOf("sign_in_provider" to provider))
        customProvider?.let { claims["providerType"] = it }
        `when`(token.claims).thenReturn(claims)
        `when`(token.uid).thenReturn(uid)
        `when`(token.email).thenReturn(email)
        return token
    }
}
