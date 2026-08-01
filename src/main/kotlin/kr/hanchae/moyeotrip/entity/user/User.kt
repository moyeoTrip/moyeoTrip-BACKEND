package kr.hanchae.moyeotrip.entity.user

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import java.time.LocalDate

@Entity
@Table(
    name = "users",
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val userRole: UserRole,
    @Column(unique = true, updatable = false)
    val email: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var signupState: SignupState = SignupState.USER_INFO_REQUIRED,
    userInformation: UserInformation? = null,
) : BaseModifiableEntity() {
    @Embedded
    var information: UserInformation? = userInformation
        protected set

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val authIdentities: MutableSet<UserAuthIdentity> = linkedSetOf()

    @Column(unique = true)
    var fcmToken: String? = null
        protected set

    @Column(nullable = false)
    var profileImageGenerationCount: Int = 0
        protected set

    fun changeFcmToken(token: String) {
        this.fcmToken = token
    }

    fun addAuthIdentity(
        providerType: ProviderType,
        providerUserId: String,
    ): UserAuthIdentity {
        check(authIdentities.none { it.providerType == providerType }) { "이미 연결된 로그인 제공자입니다." }
        return UserAuthIdentity(user = this, providerType = providerType, providerUserId = providerUserId)
            .also(authIdentities::add)
    }

    fun linkedProviders(): Set<ProviderType> = authIdentities.mapTo(linkedSetOf()) { it.providerType }

    fun recordProfileImageGeneration() {
        check(profileImageGenerationCount < MAX_PROFILE_IMAGE_GENERATION_COUNT) { "프로필 이미지 생성 횟수를 초과했습니다." }
        checkNotNull(information) { "닉네임을 설정한 사용자만 프로필 이미지를 생성할 수 있습니다." }
        profileImageGenerationCount++
    }

    fun setSignupInformation(userInformation: UserInformation) {
        this.information = userInformation
        this.signupState = SignupState.PROFILE_IMAGE_REQUIRED
    }

    fun selectProfileImage(fileName: String) {
        checkNotNull(information) { "닉네임을 설정한 사용자만 프로필 이미지를 선택할 수 있습니다." }
            .profileFileName = fileName
        signupState = SignupState.SIGNUP_COMPLETE
    }

    fun canGenerateProfileImage(): Boolean = profileImageGenerationCount < MAX_PROFILE_IMAGE_GENERATION_COUNT

    fun remainingProfileImageGenerationCount(): Int = MAX_PROFILE_IMAGE_GENERATION_COUNT - profileImageGenerationCount

    companion object {
        const val MAX_PROFILE_IMAGE_GENERATION_COUNT = 3

        fun createFirebaseUser(
            email: String?,
            nickname: String,
            nicknameColor: NicknameColor,
            gender: Gender = Gender.N,
            birthDate: LocalDate? = null,
            userRole: UserRole,
        ): User =
            User(
                email = email,
                userRole = userRole,
                signupState = SignupState.PROFILE_IMAGE_REQUIRED,
                userInformation =
                    UserInformation(
                        nickname = nickname,
                        nicknameColor = nicknameColor,
                        gender = gender,
                        birthDate = birthDate,
                    ),
            )
    }
}
