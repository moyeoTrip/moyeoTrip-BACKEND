package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateCustomCourseRequest
import kr.hanchae.moyeotrip.controller.chat.request.CustomCoursePlaceRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateMeetingInfoRequest
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomJoinApplication
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseRating
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatRoomJoinApplicationRepository
import kr.hanchae.moyeotrip.repository.ChatRoomNoticeRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TravelCoursePlaceRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRatingRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.TravelCourseTagRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class ChatRoomServiceTest {
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val applicationRepository = mock(ChatRoomJoinApplicationRepository::class.java)
    private val messageRepository = mock(ChatMessageRepository::class.java)
    private val courseRepository = mock(TravelCourseRepository::class.java)
    private val placeRepository = mock(TravelCoursePlaceRepository::class.java)
    private val tagRepository = mock(TravelCourseTagRepository::class.java)
    private val ratingRepository = mock(TravelCourseRatingRepository::class.java)
    private val tourismContentRepository = mock(TourismContentRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val noticeRepository = mock(ChatRoomNoticeRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val realtimeMessagingService = mock(RealtimeMessagingService::class.java)
    private val service =
        ChatRoomService(
            roomRepository,
            participantRepository,
            applicationRepository,
            messageRepository,
            courseRepository,
            placeRepository,
            tagRepository,
            ratingRepository,
            tourismContentRepository,
            userRepository,
            objectStorageRepository,
            noticeRepository,
            notificationService,
            realtimeMessagingService,
        )

    @Test
    fun `채팅방 참가자가 아니어도 채팅방 상세를 조회할 수 있다`() {
        val room = room(user(1L))
        `when`(roomRepository.findById(room.id)).thenReturn(Optional.of(room))
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)).thenReturn(emptyList())

        val response = service.getRoom(room.id)

        assertEquals(room.id, response.roomId)
        verify(participantRepository).findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
    }

    @Test
    fun `채팅방 참가자가 아니어도 여행 코스를 조회할 수 있다`() {
        val room = room(user(1L))
        `when`(roomRepository.findById(room.id)).thenReturn(Optional.of(room))
        `when`(ratingRepository.findAverageByCourseId(room.course.id)).thenReturn(null)
        `when`(ratingRepository.countByCourseId(room.course.id)).thenReturn(0L)

        val response = service.getRoomCourse(room.id)

        assertEquals(room.id, response.room?.roomId)
        assertEquals(room.course.id, response.course.courseId)
        assertEquals("1박 2일", response.course.travelTime)
        verifyNoInteractions(participantRepository)
    }

    @Test
    fun `공개 코스 상세 조회는 채팅방 정보 없이 재사용 가능한 코스 정보를 반환한다`() {
        val course =
            TravelCourse(
                id = 5L,
                type = TravelCourseType.PUBLIC,
                title = "울릉도 대표 코스",
                description = "바다와 산을 함께 즐기는 코스",
                tripNights = 1,
                tripDays = 2,
            )
        `when`(courseRepository.findByIdAndType(5L, TravelCourseType.PUBLIC)).thenReturn(course)
        `when`(ratingRepository.findAverageByCourseId(5L)).thenReturn(4.46)
        `when`(ratingRepository.countByCourseId(5L)).thenReturn(12L)

        val response = service.getCourse(5L)

        assertEquals("울릉도 대표 코스", response.title)
        assertEquals("바다와 산을 함께 즐기는 코스", response.description)
        assertEquals("1박 2일", response.travelTime)
        assertEquals(4.5, response.averageRating)
        assertEquals(12L, response.ratingCount)

        `when`(ratingRepository.findAverageByCourseId(5L)).thenReturn(4.04)
        assertEquals(4.0, service.getCourse(5L).averageRating)
    }

    @Test
    fun `코스 평점은 채팅방과 사용자 기준으로 갱신한다`() {
        val user = user(2L)
        val room = room(user(1L))
        val rating = TravelCourseRating(course = room.course, chatRoom = room, user = user, score = 3)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.hasCompletedTrip(10L, 2L, LocalDate.now())).thenReturn(true)
        `when`(ratingRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(rating)

        service.rateCourse(2L, 10L, 5)

        assertEquals(5, rating.score)
        verify(ratingRepository).findByChatRoomIdAndUserId(10L, 2L)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `호스트가 확정 전 커스텀 코스를 수정하면 참가자에게 알린다`() {
        val host = user(1L)
        val course = TravelCourse(id = 5L, type = TravelCourseType.CUSTOM, owner = host, title = "직접 만든 코스")
        val room = room(host, course)
        val tourismContent =
            TourismContent(contentId = 100L, contentType = TourismContentType(12, "관광지"), title = "관광지")
        val request =
            UpdateTravelCourseRequest(
                places =
                    listOf(
                        place(100L, 1, 1, 9, 0),
                        place(100L, 1, 2, 11, 0),
                        place(100L, 2, 1, 9, 30),
                        place(100L, 2, 2, 14, 0),
                    ),
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(tourismContentRepository.findByContentId(100L)).thenReturn(tourismContent)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }
        `when`(ratingRepository.findAverageByCourseId(5L)).thenReturn(null)
        `when`(ratingRepository.countByCourseId(5L)).thenReturn(0L)

        val response = service.updateRoomCourse(1L, 10L, request)

        assertEquals(4, response.places.size)
        assertEquals(2, response.places.last().dayNumber)
        assertEquals(LocalTime.of(14, 0), response.places.last().visitTime)
        verify(placeRepository).deleteAllByCourseId(5L)
        verify(notificationService).notifyCourseUpdated(room, 0L)
    }

    @Test
    fun `하루 방문지가 두 곳 미만이면 커스텀 코스를 수정할 수 없다`() {
        val host = user(1L)
        val course = TravelCourse(id = 5L, type = TravelCourseType.CUSTOM, owner = host, title = "직접 만든 코스")
        val room = room(host, course)
        val request =
            UpdateTravelCourseRequest(
                places =
                    listOf(
                        place(100L, 1, 1, 9, 0),
                        place(100L, 1, 2, 11, 0),
                        place(100L, 2, 1, 9, 30),
                    ),
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)

        val exception = assertThrows(BaseException::class.java) { service.updateRoomCourse(1L, 10L, request) }

        assertEquals(ErrorCode.INVALID_TRAVEL_COURSE_SCHEDULE, exception.errorCode)
        verifyNoInteractions(tourismContentRepository)
    }

    @Test
    fun `당일치기 일정에는 종료 날짜를 입력할 수 없다`() {
        val request = createRoomRequest(TripType.DAY_TRIP, endDate = LocalDate.now().plusDays(1))

        val exception = assertThrows(BaseException::class.java) { service.createRoom(1L, request) }

        assertEquals(ErrorCode.INVALID_TRIP_SCHEDULE, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `숙박 일정에는 당일치기 시작 종료 시간을 입력할 수 없다`() {
        val request =
            createRoomRequest(
                tripType = TripType.OVERNIGHT,
                endDate = LocalDate.now().plusDays(1),
                dayTripStartTime = LocalTime.of(9, 0),
                dayTripEndTime = LocalTime.of(18, 0),
            )

        val exception = assertThrows(BaseException::class.java) { service.createRoom(1L, request) }

        assertEquals(ErrorCode.INVALID_TRIP_SCHEDULE, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `등록 코스를 사용하는 방도 호스트가 확정 전에 집합 정보만 수정할 수 있다`() {
        val room = room(user(1L))
        val changedDateTime = LocalDateTime.now().plusDays(9)
        val request =
            UpdateMeetingInfoRequest(
                meetingLatitude = 37.5547,
                meetingLongitude = 126.9706,
                meetingDetails = " 서울역 1번 출구 ",
                meetingDateTime = changedDateTime,
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        service.updateMeetingInfo(1L, 10L, request)

        assertEquals(37.5547, room.meetingLatitude)
        assertEquals(126.9706, room.meetingLongitude)
        assertEquals("서울역 1번 출구", room.meetingDetails)
        assertEquals(changedDateTime, room.meetingDateTime)
        verify(notificationService).notifyMeetingInfoUpdated(room, 0L)
        verifyNoInteractions(placeRepository)
    }

    @Test
    fun `등록 코스의 경로는 확정 전에도 수정할 수 없다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)

        val exception =
            assertThrows(BaseException::class.java) {
                service.updateRoomCourse(1L, 10L, validTwoDayCourseUpdate())
            }

        assertEquals(ErrorCode.TRAVEL_COURSE_NOT_EDITABLE, exception.errorCode)
        verifyNoInteractions(placeRepository)
    }

    @Test
    fun `여행이 확정되면 직접 만든 코스의 경로와 집합 정보를 수정할 수 없다`() {
        val host = user(1L)
        val course = TravelCourse(id = 5L, type = TravelCourseType.CUSTOM, owner = host, title = "직접 만든 코스")
        val room = room(host, course)
        room.confirm()
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)

        val courseException =
            assertThrows(BaseException::class.java) {
                service.updateRoomCourse(1L, 10L, validTwoDayCourseUpdate())
            }
        val meetingException =
            assertThrows(BaseException::class.java) {
                service.updateMeetingInfo(
                    1L,
                    10L,
                    UpdateMeetingInfoRequest(meetingDateTime = LocalDateTime.now().plusDays(9)),
                )
            }

        assertEquals(ErrorCode.TRAVEL_COURSE_NOT_EDITABLE, courseException.errorCode)
        assertEquals(ErrorCode.MEETING_INFO_NOT_EDITABLE, meetingException.errorCode)
    }

    @Test
    fun `호스트가 신청자를 승인할 때 정원이 가득 차면 승인된 대기열로 이동한다`() {
        val host = user(1L)
        val applicant = user(3L)
        val room = room(host)
        val application =
            ChatRoomJoinApplication(id = 30L, chatRoom = room, user = applicant, applicationMessage = "함께 가고 싶어요")
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(applicationRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(application)
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(3L)
        `when`(applicationRepository.countByChatRoomIdAndStatus(10L, JoinApplicationStatus.WAITLISTED)).thenReturn(1L)

        val response = service.approveApplication(1L, 10L, 30L)

        assertEquals("WAITLISTED", response.result.name)
        assertEquals(JoinApplicationStatus.WAITLISTED, application.status)
        assertEquals(1, response.waitlistPosition)
    }

    @Test
    fun `참가자가 나가면 호스트가 승인한 대기자 중 첫 사용자가 자동 참가한다`() {
        val host = user(1L)
        val leavingUser = user(2L)
        val waitingUser = user(3L)
        val room = room(host)
        val participant = ChatRoomParticipant(id = 20L, chatRoom = room, user = leavingUser, role = ChatParticipantRole.MEMBER)
        val waiting =
            ChatRoomJoinApplication(
                id = 30L,
                chatRoom = room,
                user = waitingUser,
                applicationMessage = "신청합니다",
                status = JoinApplicationStatus.WAITLISTED,
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(
            applicationRepository.findFirstByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(
                10L,
                JoinApplicationStatus.WAITLISTED,
            ),
        ).thenReturn(waiting)
        `when`(participantRepository.save(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(participantRepository.saveAndFlush(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        val response = service.leaveRoom(2L, 10L)

        assertEquals(3L, response.promotedUserId)
        verify(applicationRepository).delete(waiting)
        verify(participantRepository).saveAndFlush(any(ChatRoomParticipant::class.java))
    }

    private fun user(id: Long) = User(id = id, userRole = UserRole.ROLE_USER)

    private fun place(
        contentId: Long,
        dayNumber: Int,
        sequence: Int,
        hour: Int,
        minute: Int,
    ) = CustomCoursePlaceRequest(contentId, dayNumber, sequence, LocalTime.of(hour, minute))

    private fun createRoomRequest(
        tripType: TripType,
        endDate: LocalDate?,
        dayTripStartTime: LocalTime? = LocalTime.of(9, 0),
        dayTripEndTime: LocalTime? = LocalTime.of(18, 0),
    ) = CreateChatRoomRequest(
        title = "테스트 여행",
        maxParticipants = 4,
        tripType = tripType,
        startDate = LocalDate.now(),
        endDate = endDate,
        recruitmentDeadlineDate = LocalDate.now(),
        dayTripStartTime = dayTripStartTime,
        dayTripEndTime = dayTripEndTime,
        meetingDateTime = LocalDateTime.now(),
        courseType = TravelCourseType.CUSTOM,
        customCourse =
            CreateCustomCourseRequest(
                title = "테스트 코스",
                places = listOf(place(100L, 1, 1, 9, 0), place(101L, 1, 2, 11, 0)),
            ),
    )

    private fun validTwoDayCourseUpdate() =
        UpdateTravelCourseRequest(
            places =
                listOf(
                    place(100L, 1, 1, 9, 0),
                    place(101L, 1, 2, 11, 0),
                    place(102L, 2, 1, 9, 0),
                    place(103L, 2, 2, 11, 0),
                ),
        )

    private fun room(
        host: User,
        course: TravelCourse = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "울릉도 대표 코스"),
    ) = ChatRoom(
        id = 10L,
        host = host,
        course = course,
        roomTitle = "울릉도 여행",
        maxParticipants = 3,
        startDate = LocalDate.now().plusDays(10),
        endDate = LocalDate.now().plusDays(11),
        recruitmentDeadlineDate = LocalDate.now().plusDays(5),
        meetingLatitude = 36.0322,
        meetingLongitude = 129.3747,
        meetingDateTime = LocalDateTime.now().plusDays(10),
        participationFee = 100000L,
    )
}
