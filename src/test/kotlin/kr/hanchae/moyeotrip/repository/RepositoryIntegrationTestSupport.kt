package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.support.ContainerIntegrationTestSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.LocalTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(KotlinJdslAutoConfiguration::class)
@TestPropertySource(properties = ["spring.cloud.aws.parameterstore.enabled=false"])
abstract class RepositoryIntegrationTestSupport : ContainerIntegrationTestSupport() {
    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var chatRoomRepository: ChatRoomRepository

    @Autowired
    protected lateinit var travelCourseRepository: TravelCourseRepository

    protected fun savedUser(): User = userRepository.saveAndFlush(User(userRole = UserRole.ROLE_USER))

    protected fun savedCourse(
        title: String = "테스트 코스 ${System.nanoTime()}",
        type: TravelCourseType = TravelCourseType.PUBLIC,
    ): TravelCourse = travelCourseRepository.saveAndFlush(TravelCourse(type = type, title = title))

    protected fun savedRoom(
        host: User,
        course: TravelCourse,
        title: String = "테스트 채팅방 ${System.nanoTime()}",
        startDate: LocalDate = LocalDate.now().plusDays(1),
        recruitmentDeadlineDate: LocalDate = startDate,
        status: ChatRoomStatus = ChatRoomStatus.RECRUITING,
    ): ChatRoom {
        val room =
            ChatRoom(
                host = host,
                course = course,
                roomTitle = title,
                maxParticipants = 3,
                startDate = startDate,
                recruitmentDeadlineDate = recruitmentDeadlineDate,
                dayTripStartTime = LocalTime.of(9, 0),
                dayTripEndTime = LocalTime.of(18, 0),
                meetingDateTime = startDate.atTime(8, 30),
            )
        if (status == ChatRoomStatus.CONFIRMED) room.confirm()
        return chatRoomRepository.saveAndFlush(room)
    }
}
