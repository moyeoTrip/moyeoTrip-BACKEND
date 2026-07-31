package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun existsByInformation_Nickname(nickName: String): Boolean
    fun findByProviderTypeAndProviderUserId(providerType: ProviderType,providerUserId: String): User?
    fun existsByProviderTypeAndProviderUserId(providerType: ProviderType,providerUserId: String): Boolean
    fun findByEmail(email: String): User?
}
