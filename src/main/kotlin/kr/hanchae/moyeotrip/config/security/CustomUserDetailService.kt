package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(userIdStr: String): UserDetails {
        val userId = userIdStr.toLong()
        return SecurityUser(userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) })
    }
}

class SecurityUser(
    val user: User,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = listOf()

    override fun getPassword(): String = ""

    override fun getUsername(): String = ""

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
