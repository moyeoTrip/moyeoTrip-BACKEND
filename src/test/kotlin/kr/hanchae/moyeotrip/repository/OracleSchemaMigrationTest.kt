package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import jakarta.persistence.EntityManager
import kr.hanchae.moyeotrip.entity.user.AdjectiveProfile
import kr.hanchae.moyeotrip.entity.user.AnimalProfile
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.support.ContainerIntegrationTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(KotlinJdslAutoConfiguration::class)
class OracleSchemaMigrationTest : ContainerIntegrationTestSupport() {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `모든 VARCHAR2 컬럼은 문자 길이 기준을 사용한다`() {
        val byteSemanticsColumnCount =
            entityManager
                .createNativeQuery(
                    """
                    SELECT COUNT(*)
                    FROM user_tab_columns
                    WHERE table_name IN ('USERS', 'USER_AUTH_IDENTITIES', 'USER_PROFILE_IMAGES')
                      AND data_type = 'VARCHAR2'
                      AND char_used <> 'C'
                    """.trimIndent(),
                ).singleResult as Number

        assertEquals(0L, byteSemanticsColumnCount.toLong())
    }

    @Test
    fun `가장 긴 닉네임 후보를 Oracle에 저장할 수 있다`() {
        val longestAdjective = AdjectiveProfile.entries.maxBy { it.word.length }.word
        val longestAnimal = AnimalProfile.entries.maxBy { it.word.length }.word
        val nickname = "$longestAdjective $longestAnimal 0000"

        assertTrue(nickname.toByteArray(Charsets.UTF_8).size > 24)
        assertTrue(nickname.length <= 24)

        val user =
            User.createFirebaseUser(
                email = "nickname-length-test@moyeotrip.test",
                nickname = nickname,
                nicknameColor = NicknameColor.BLUE,
                userRole = UserRole.ROLE_USER,
            )

        val savedUser = userRepository.saveAndFlush(user)

        assertEquals(nickname, savedUser.information?.nickname)
    }
}
