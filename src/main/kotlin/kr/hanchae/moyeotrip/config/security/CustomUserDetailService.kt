package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(email: String): UserDetails {
        val user =
            userRepository.findByEmail(email) ?: throw BaseException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.errorMessage)

        return CustomUserDto(
            id = user.id.toString(),
            password = user.password!!,
            authorities = listOf(SimpleGrantedAuthority(user.userRole.name)),
        )
    }
}

class CustomUserDto(
    id: String,
    password: String,
    authorities: Collection<SimpleGrantedAuthority>,
) : User(id, password, authorities)
