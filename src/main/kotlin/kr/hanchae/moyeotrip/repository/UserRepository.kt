package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun existsByInformationNickname(nickName: String): Boolean

    fun findByEmail(email: String): User?
}
