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

    companion object {
        fun createFirebaseUser(
            email: String?,
            nickname: String,
            userRole: UserRole,
        ): User =
            User(
                email = email,
                userRole = userRole,
                signupState = SignupState.SIGNUP_COMPLETE,
                userInformation =
                    UserInformation(
                        nickname = nickname,
                        gender = Gender.N,
                    ),
            )
    }

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

    fun changeSignupStateComplete(userInformation: UserInformation) {
        this.information = userInformation
        this.signupState = SignupState.SIGNUP_COMPLETE
    }
}
