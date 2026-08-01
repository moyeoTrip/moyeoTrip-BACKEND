package kr.hanchae.moyeotrip.client

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.springframework.stereotype.Component

data class FirebaseIdentity(
    val uid: String,
    val email: String?,
    val providerType: ProviderType,
)

@Component
class FirebaseAuthenticationClient(
    private val firebaseAuth: FirebaseAuth,
) {
    fun verifyIdToken(idToken: String): FirebaseIdentity =
        try {
            firebaseAuth.verifyIdToken(idToken, true).toIdentity()
        } catch (exception: FirebaseAuthException) {
            throw BaseException(ErrorCode.INVALID_FIREBASE_TOKEN, exception.message)
        } catch (exception: IllegalArgumentException) {
            throw BaseException(ErrorCode.INVALID_FIREBASE_TOKEN, exception.message)
        }

    fun createKakaoCustomToken(kakaoProviderUserId: String): String =
        try {
            firebaseAuth.createCustomToken(
                kakaoProviderUserId,
                mapOf("providerType" to ProviderType.KAKAO.name),
            )
        } catch (exception: FirebaseAuthException) {
            throw BaseException(ErrorCode.FIREBASE_AUTH_ERROR, exception.message)
        }

    private fun FirebaseToken.toIdentity(): FirebaseIdentity {
        val firebaseClaims = claims["firebase"] as? Map<*, *>
        val signInProvider = firebaseClaims?.get("sign_in_provider") as? String
        val providerType =
            when (signInProvider) {
                "password", "emailLink" -> ProviderType.EMAIL
                "apple.com" -> ProviderType.APPLE
                "google.com" -> ProviderType.GOOGLE
                "custom" -> parseCustomProvider(claims["providerType"] as? String)
                else -> throw BaseException(ErrorCode.INVALID_AUTH_PROVIDER, "지원하지 않는 Firebase 로그인 제공자입니다: $signInProvider")
            }
        return FirebaseIdentity(uid = uid, email = email, providerType = providerType)
    }

    private fun parseCustomProvider(provider: String?): ProviderType =
        runCatching { ProviderType.valueOf(provider.orEmpty()) }
            .getOrNull()
            ?.takeIf { it == ProviderType.KAKAO }
            ?: throw BaseException(ErrorCode.INVALID_AUTH_PROVIDER, "유효하지 않은 Firebase 커스텀 로그인 제공자입니다.")
}
