package kr.hanchae.moyeotrip.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
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
    @Column(nullable = false, updatable = false)
    val providerType: ProviderType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val userRole: UserRole,
    @Column(updatable = false)
    val providerUserId: String? = null,
    @Column(unique = true, updatable = false)
    val email: String? = null,
    val password: String? = null,
    @Column(nullable = false)
    var signupState: SignupState = SignupState.USER_INFO_REQUIRED,
    userInformation: UserInformation? = null
) : BaseModifiableEntity() {
    @Embedded
    var information: UserInformation? = userInformation
        protected set

    @Column(unique = true)
    var fcmToken: String? = null
        protected set

    companion object {
        fun createSocailUser(
            providerType: ProviderType,
            providerUserId: String,
            userRole: UserRole,
        ): User = User(
            providerType = providerType,
            providerUserId = providerUserId,
            userRole = userRole,
        )

        fun createEmailUser(
            email: String,
            password: String,
            userRole: UserRole,
        ): User = User(
            providerType = ProviderType.EMAIL,
            userRole = userRole,
            email = email,
            password = password
        )
    }
    fun changeFcmToken(token: String) {
        this.fcmToken = token
    }

    fun changeSignupStateComplete(userInformation: UserInformation) {
        this.information = userInformation
        this.signupState = SignupState.SIGNUP_COMPLETE
    }
}
