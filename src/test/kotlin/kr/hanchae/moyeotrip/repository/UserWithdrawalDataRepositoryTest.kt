package kr.hanchae.moyeotrip.repository

import jakarta.persistence.EntityManager
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseLike
import kr.hanchae.moyeotrip.entity.tour.TravelCourseRating
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Friendship
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.TravelCompanion
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@Import(UserWithdrawalDataRepository::class)
class UserWithdrawalDataRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var withdrawalRepository: UserWithdrawalDataRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `탈퇴 활동을 지우고 좋아요 또는 완료 여행 이력이 있는 공개 코스만 보존한다`() {
        val owner = savedProfileUser("코스 만든 여행자")
        val otherUser = savedProfileUser("코스를 좋아한 여행자")
        val likedCourse = savedOwnedPublicCourse(owner, "좋아요 코스")
        val historyCourse = savedOwnedPublicCourse(owner, "여행 이력 코스")
        val unusedCourse = savedOwnedPublicCourse(owner, "미사용 코스")
        entityManager.persist(TravelCourseLike(course = likedCourse, user = otherUser))

        val completedRoom =
            savedRoom(
                host = owner,
                course = historyCourse,
                startDate = LocalDate.now().minusDays(2),
                recruitmentDeadlineDate = LocalDate.now().minusDays(3),
                status = kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus.CONFIRMED,
            )
        entityManager.persist(ChatRoomParticipant(chatRoom = completedRoom, user = owner, role = ChatParticipantRole.HOST))
        entityManager.persist(TravelCompanion(owner = owner, companion = otherUser, chatRoom = completedRoom))
        entityManager.persist(Friendship(firstUser = owner, secondUser = otherUser))
        entityManager.persist(TravelCourseRating(course = historyCourse, chatRoom = completedRoom, user = owner, score = 5))
        val feed = Feed(author = owner, chatRoom = completedRoom, content = "삭제될 여행 피드", visibility = FeedVisibility.PUBLIC)
        feed.addImage("feed/image/withdrawal.webp", 0)
        entityManager.persist(feed)
        entityManager.flush()

        val storedObjectKeys =
            withdrawalRepository.removePersonalActivity(
                owner.id,
                owner.information?.nickname,
                LocalDate.now().atStartOfDay(),
            )
        entityManager.clear()

        assertEquals(listOf("feed/image/withdrawal.webp"), storedObjectKeys.feedImages)
        assertTrue(travelCourseRepository.existsById(likedCourse.id))
        assertTrue(travelCourseRepository.existsById(historyCourse.id))
        assertFalse(travelCourseRepository.existsById(unusedCourse.id))
        assertEquals("코스 만든 여행자", creatorNickname(likedCourse.id))
        assertEquals(1L, retainedAfterWithdrawal(likedCourse.id))
        assertEquals(1L, retainedAfterWithdrawal(historyCourse.id))
        assertEquals(0L, count("feeds", "author_id", owner.id))
        assertEquals(0L, count("friendships", "first_user_id", owner.id))
        assertEquals(0L, count("travel_companions", "owner_id", owner.id))
        assertEquals(0L, count("chat_room_participants", "user_id", owner.id))
        assertEquals(0L, count("travel_course_ratings", "user_id", owner.id))
    }

    @Test
    fun `영구 삭제 전 참조를 정리하면 공개 코스 닉네임은 남고 사용자와 커스텀 코스는 삭제된다`() {
        val owner = savedProfileUser("탈퇴한 코스 작성자")
        val otherUser = savedProfileUser("좋아요 사용자")
        val publicCourse = savedOwnedPublicCourse(owner, "남을 공개 코스")
        val customCourse =
            travelCourseRepository.saveAndFlush(
                TravelCourse(type = TravelCourseType.CUSTOM, owner = owner, title = "삭제될 커스텀 코스"),
            )
        entityManager.persist(TravelCourseLike(course = publicCourse, user = otherUser))
        savedRoom(host = owner, course = customCourse)
        entityManager.flush()

        withdrawalRepository.removePersonalActivity(owner.id, owner.information?.nickname, LocalDate.now().atStartOfDay())
        withdrawalRepository.preparePermanentDeletion(owner.id, owner.information?.nickname)
        entityManager.clear()
        userRepository.deleteById(owner.id)
        userRepository.flush()
        entityManager.clear()

        assertFalse(userRepository.existsById(owner.id))
        assertFalse(travelCourseRepository.existsById(customCourse.id))
        assertTrue(travelCourseRepository.existsById(publicCourse.id))
        assertEquals("탈퇴한 코스 작성자", creatorNickname(publicCourse.id))
        assertEquals(0L, ownerCount(publicCourse.id))
    }

    private fun savedProfileUser(nickname: String): User =
        userRepository.saveAndFlush(
            User(
                userRole = UserRole.ROLE_USER,
                userInformation =
                    UserInformation(
                        nickname = nickname,
                        nicknameColor = NicknameColor.MINT,
                        gender = Gender.N,
                    ),
            ),
        )

    private fun savedOwnedPublicCourse(
        owner: User,
        title: String,
    ): TravelCourse =
        travelCourseRepository.saveAndFlush(
            TravelCourse(type = TravelCourseType.PUBLIC, owner = owner, title = title),
        )

    private fun creatorNickname(courseId: Long): String? =
        jdbcTemplate.queryForObject(
            "SELECT creator_nickname FROM travel_courses WHERE id = ?",
            String::class.java,
            courseId,
        )

    private fun retainedAfterWithdrawal(courseId: Long): Long =
        checkNotNull(
            jdbcTemplate.queryForObject(
                "SELECT retained_after_owner_withdrawal FROM travel_courses WHERE id = ?",
                Long::class.java,
                courseId,
            ),
        )

    private fun ownerCount(courseId: Long): Long =
        checkNotNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(owner_id) FROM travel_courses WHERE id = ?",
                Long::class.java,
                courseId,
            ),
        )

    private fun count(
        table: String,
        column: String,
        userId: Long,
    ): Long =
        checkNotNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM $table WHERE $column = ?",
                Long::class.java,
                userId,
            ),
        )
}
