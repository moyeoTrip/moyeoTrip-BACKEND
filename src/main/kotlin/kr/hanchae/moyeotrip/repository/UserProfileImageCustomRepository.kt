package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserProfileImage

interface UserProfileImageCustomRepository {
    fun findFileNamesByUserIdOrderByCreatedDateTimeAsc(userId: Long): List<String>
}

class UserProfileImageCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : UserProfileImageCustomRepository {
    override fun findFileNamesByUserIdOrderByCreatedDateTimeAsc(userId: Long): List<String> =
        kotlinJdslJpqlExecutor
            .findAll {
                val profileImage = entity(UserProfileImage::class)

                select(profileImage.path(UserProfileImage::fileName))
                    .from(profileImage)
                    .where(profileImage.path(UserProfileImage::user).path(User::id).eq(userId))
                    .orderBy(profileImage.path(UserProfileImage::createdDateTime).asc())
            }.filterNotNull()
}
