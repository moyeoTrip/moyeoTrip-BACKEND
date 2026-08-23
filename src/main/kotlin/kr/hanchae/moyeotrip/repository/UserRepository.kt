package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import jakarta.persistence.LockModeType
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

@Repository
interface UserRepository :
    JpaRepository<User, Long>,
    UserCustomRepository {
    fun existsByInformationNickname(nickName: String): Boolean

    fun findByEmail(email: String): User?

    fun findByFcmToken(fcmToken: String): User?
}

interface UserCustomRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdForUpdate(id: Long): User?
}

class UserCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : UserCustomRepository {
    override fun findByIdForUpdate(id: Long): User? =
        kotlinJdslJpqlExecutor
            .findAll {
                val user = entity(User::class)

                select(user)
                    .from(user)
                    .where(user.path(User::id).eq(id))
            }.firstOrNull()
}
