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
    @Column(nullable = false, updatable = false)
    val providerUserId: String,
    userInformation: UserInformation,
) : BaseModifiableEntity() {
    @Embedded
    var information: UserInformation = userInformation
        protected set

    @Column(unique = true)
    var fcmToken: String? = null
        protected set

    companion object {
        fun createUser(
            email: String?,
            providerType: ProviderType,
            providerUserId: String,
            userRole: UserRole,
            gender: Gender,
            nickname: String,
            profileFileName: String? = null,
        ): User = User(
            providerType = providerType,
            providerUserId = providerUserId,
            userRole = userRole,
            userInformation = UserInformation(
                email = email,
                nickname = nickname,
                gender = gender,
                profileFileName = profileFileName
            ),
        )
    }
    fun changeFcmToken(token: String) {
        this.fcmToken = token
    }
}
