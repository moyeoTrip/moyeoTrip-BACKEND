package kr.hanchae.moyeotrip.entity.user

import kr.hanchae.moyeotrip.entity.tour.LegalDongCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class UserEntityTest {
    @Test
    fun `FCM 토큰은 일치할 때만 해제한다`() {
        val user = user()
        user.changeFcmToken("token")

        assertFalse(user.clearFcmTokenIfMatches("other-token"))
        assertEquals("token", user.fcmToken)
        assertTrue(user.clearFcmTokenIfMatches("token"))
        assertNull(user.fcmToken)
    }

    @Test
    fun `로그인 시각을 기록한다`() {
        val user = user()
        val at = LocalDateTime.of(2026, 8, 30, 12, 0)

        user.recordLogin(at)

        assertEquals(at, user.lastLoginDateTime)
    }

    @Test
    fun `탈퇴하면 토큰을 해제하고 유예 기간 안에만 복구할 수 있다`() {
        val user = user()
        val withdrawnAt = LocalDateTime.of(2026, 8, 1, 0, 0)
        user.changeFcmToken("token")

        user.withdraw(withdrawnAt)

        assertTrue(user.isWithdrawn())
        assertNull(user.fcmToken)
        assertTrue(user.canRestore(withdrawnAt.plusDays(29)))
        assertFalse(user.canRestore(withdrawnAt.plusDays(30)))
        assertThrows(IllegalStateException::class.java) { user.withdraw(withdrawnAt.plusDays(1)) }

        user.restore()
        assertFalse(user.isWithdrawn())
        assertFalse(user.canRestore(withdrawnAt.plusDays(1)))
        assertThrows(IllegalStateException::class.java) { user.restore() }
    }

    @Test
    fun `로그인 제공자는 종류별로 한 번만 연결한다`() {
        val user = user()

        user.addAuthIdentity(ProviderType.EMAIL, "firebase-id")
        user.addAuthIdentity(ProviderType.KAKAO, "kakao-id")

        assertEquals(setOf(ProviderType.EMAIL, ProviderType.KAKAO), user.linkedProviders())
        assertThrows(IllegalStateException::class.java) {
            user.addAuthIdentity(ProviderType.KAKAO, "other-kakao-id")
        }
    }

    @Test
    fun `프로필 정보가 있어야 이미지를 생성하고 최대 세 번까지만 허용한다`() {
        val incomplete = user()
        assertThrows(IllegalStateException::class.java) { incomplete.recordProfileImageGeneration() }

        val user = profiledUser()
        repeat(User.MAX_PROFILE_IMAGE_GENERATION_COUNT) { user.recordProfileImageGeneration() }

        assertEquals(3, user.profileImageGenerationCount)
        assertEquals(0, user.remainingProfileImageGenerationCount())
        assertFalse(user.canGenerateProfileImage())
        assertThrows(IllegalStateException::class.java) { user.recordProfileImageGeneration() }
    }

    @Test
    fun `가입 정보와 프로필 이미지 선택으로 가입 상태를 진행한다`() {
        val user = user()
        assertThrows(IllegalStateException::class.java) { user.selectProfileImage("profile.webp") }

        user.setSignupInformation(information())
        assertEquals(SignupState.PROFILE_IMAGE_REQUIRED, user.signupState)
        assertTrue(user.canGenerateProfileImage())

        user.selectProfileImage("profile.webp")
        assertEquals(SignupState.SIGNUP_COMPLETE, user.signupState)
        assertEquals("profile.webp", user.information?.profileFileName)
    }

    @Test
    fun `프로필 수정은 소개 여행 스타일 관심 지역 생년월일 성별을 교체한다`() {
        val incomplete = user()
        assertThrows(IllegalStateException::class.java) {
            incomplete.updateProfile(null, emptySet(), emptySet(), LocalDate.of(2000, 1, 1), Gender.N)
        }

        val user = profiledUser()
        val style = TravelStyle(id = 1L, label = "힐링")
        val region = LegalDongCode(id = 1L, regionCode = "47", signguCode = "110", regionName = "경상북도", signguName = "포항시")
        val birthDate = LocalDate.of(1995, 5, 5)

        user.updateProfile("천천히 여행해요", setOf(style), setOf(region), birthDate, Gender.F)

        assertEquals("천천히 여행해요", user.information?.introduction)
        assertEquals(birthDate, user.information?.birthDate)
        assertEquals(Gender.F, user.information?.gender)
        assertEquals(setOf(style), user.travelStyles)
        assertEquals(setOf(region), user.interestedRegions)

        user.updateProfile(null, emptySet(), emptySet(), birthDate, Gender.N)
        assertTrue(user.travelStyles.isEmpty())
        assertTrue(user.interestedRegions.isEmpty())
    }

    @Test
    fun `매너 평점은 0에서 5 사이만 허용한다`() {
        val user = user()

        assertThrows(IllegalArgumentException::class.java) { user.updateMannerRating(-0.1) }
        assertThrows(IllegalArgumentException::class.java) { user.updateMannerRating(5.1) }
        user.updateMannerRating(0.0)
        assertEquals(0.0, user.mannerRating)
        user.updateMannerRating(5.0)
        assertEquals(5.0, user.mannerRating)
    }

    @Test
    fun `Firebase 사용자 팩토리는 프로필 선택 대기 상태와 입력 정보를 만든다`() {
        val user =
            User.createFirebaseUser(
                email = null,
                nickname = "따스한 사슴",
                nicknameColor = NicknameColor.GREEN,
                gender = Gender.N,
                birthDate = null,
                userRole = UserRole.ROLE_USER,
            )

        assertEquals(SignupState.PROFILE_IMAGE_REQUIRED, user.signupState)
        assertEquals("따스한 사슴", user.information?.nickname)
        assertNull(user.information?.birthDate)
    }

    private fun user() = User(id = 1L, userRole = UserRole.ROLE_USER)

    private fun profiledUser() = User(id = 1L, userRole = UserRole.ROLE_USER, userInformation = information())

    private fun information() =
        UserInformation(
            nickname = "따스한 사슴",
            nicknameColor = NicknameColor.GREEN,
            gender = Gender.N,
        )
}
