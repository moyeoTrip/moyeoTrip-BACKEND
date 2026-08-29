package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@Service
class CustomUserDetailService(
    private val userRepository: UserRepository,
) {
    fun loadUserById(userId: Long): UserDetails {
        val user =
            userRepository.findById(userId).orElseThrow {
                BaseException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.errorMessage)
            }
        if (user.isWithdrawn()) {
            throw BaseException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.errorMessage)
        }
        return CustomUserDto(
            id = user.id.toString(),
            password = "",
            authorities = listOf(SimpleGrantedAuthority(user.userRole.name)),
            signupState = user.signupState,
            hasProfileImage = user.information?.profileFileName?.isNotBlank() == true,
        )
    }
}

class CustomUserDto(
    id: String,
    password: String,
    authorities: Collection<SimpleGrantedAuthority>,
    val signupState: SignupState,
    val hasProfileImage: Boolean,
) : User(id, password, authorities)
