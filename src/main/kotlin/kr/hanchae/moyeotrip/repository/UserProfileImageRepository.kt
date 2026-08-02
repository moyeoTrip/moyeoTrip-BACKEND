package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.UserProfileImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserProfileImageRepository :
    JpaRepository<UserProfileImage, Long>,
    UserProfileImageCustomRepository {
    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): UserProfileImage?

    fun findAllByUserIdOrderByCreatedDateTimeAsc(userId: Long): List<UserProfileImage>
}
