package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.UserAuthIdentity
import org.springframework.data.jpa.repository.JpaRepository

interface UserAuthIdentityRepository : JpaRepository<UserAuthIdentity, Long> {
    fun findByProviderTypeAndProviderUserId(
        providerType: ProviderType,
        providerUserId: String,
    ): UserAuthIdentity?

    fun existsByUserIdAndProviderType(
        userId: Long,
        providerType: ProviderType,
    ): Boolean

    fun findAllByUserId(userId: Long): List<UserAuthIdentity>
}
