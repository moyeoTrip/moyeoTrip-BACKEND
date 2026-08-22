package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserProfileImage
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserProfileImageRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var userProfileImageRepository: UserProfileImageRepository

    @Nested
    inner class FindFileNamesByUserIdOrderByCreatedDateTimeAsc {
        @Test
        fun `해당 사용자의 이미지만 생성 순서대로 반환한다`() {
            val user = userRepository.saveAndFlush(User(userRole = UserRole.ROLE_USER))
            val otherUser = userRepository.saveAndFlush(User(userRole = UserRole.ROLE_USER))

            userProfileImageRepository.saveAndFlush(
                UserProfileImage(user = user, fileName = "user/profile/image/first.png"),
            )
            Thread.sleep(10)
            userProfileImageRepository.saveAndFlush(
                UserProfileImage(user = user, fileName = "user/profile/image/second.png"),
            )
            userProfileImageRepository.saveAndFlush(
                UserProfileImage(user = otherUser, fileName = "user/profile/image/other-user.png"),
            )

            val fileNames = userProfileImageRepository.findFileNamesByUserIdOrderByCreatedDateTimeAsc(user.id)

            assertEquals(
                listOf(
                    "user/profile/image/first.png",
                    "user/profile/image/second.png",
                ),
                fileNames,
            )
        }
    }
}
