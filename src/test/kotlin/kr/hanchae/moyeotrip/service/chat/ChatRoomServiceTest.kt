package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.controller.chat.request.CreateChatPollRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateCustomCourseRequest
import kr.hanchae.moyeotrip.controller.chat.request.CustomCoursePlaceRequest
import kr.hanchae.moyeotrip.controller.chat.request.JoinChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.MyChatRoomFilter
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateMeetingInfoRequest
import kr.hanchae.moyeotrip.controller.chat.response.TravelRoadmapProgress
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatPollOption
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomFavorite
import kr.hanchae.moyeotrip.entity.chat.ChatRoomJoinApplication
import kr.hanchae.moyeotrip.entity.chat.ChatRoomKickHistory
import kr.hanchae.moyeotrip.entity.chat.ChatRoomNotice
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.chat.GenderRestriction
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.chat.JoinApprovalMode
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseRating
import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatPollOptionRepository
import kr.hanchae.moyeotrip.repository.ChatPollVoteRepository
import kr.hanchae.moyeotrip.repository.ChatRoomFavoriteRepository
import kr.hanchae.moyeotrip.repository.ChatRoomJoinApplicationRepository
import kr.hanchae.moyeotrip.repository.ChatRoomKickHistoryRepository
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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class ChatRoomServiceTest {
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val applicationRepository = mock(ChatRoomJoinApplicationRepository::class.java)
    private val messageRepository = mock(ChatMessageRepository::class.java)
    private val pollOptionRepository = mock(ChatPollOptionRepository::class.java)
    private val pollVoteRepository = mock(ChatPollVoteRepository::class.java)
    private val courseRepository = mock(TravelCourseRepository::class.java)
    private val placeRepository = mock(TravelCoursePlaceRepository::class.java)
    private val tagRepository = mock(TravelCourseTagRepository::class.java)
    private val ratingRepository = mock(TravelCourseRatingRepository::class.java)
    private val favoriteRepository = mock(ChatRoomFavoriteRepository::class.java)
    private val kickHistoryRepository = mock(ChatRoomKickHistoryRepository::class.java)
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
            pollOptionRepository,
            pollVoteRepository,
            courseRepository,
            placeRepository,
            tagRepository,
            ratingRepository,
            favoriteRepository,
            kickHistoryRepository,
            tourismContentRepository,
            userRepository,
            objectStorageRepository,
            noticeRepository,
            notificationService,
            realtimeMessagingService,
        )

    @Test
    fun `채팅 사진은 20MB를 초과하면 공유할 수 없다`() {
        val image = mock(MultipartFile::class.java)
        `when`(image.isEmpty).thenReturn(false)
        `when`(image.size).thenReturn(20L * 1024 * 1024 + 1)
        `when`(image.contentType).thenReturn("image/jpeg")

        val exception =
            assertThrows(BaseException::class.java) {
                service.shareImage(2L, 10L, image, null)
            }

        assertEquals(ErrorCode.BAD_REQUEST, exception.errorCode)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `같은 채팅방 참가자가 아닌 사용자는 멘션할 수 없다`() {
        val member = user(2L)
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L)).thenReturn(listOf(participant))

        val exception =
            assertThrows(BaseException::class.java) {
                service.sendMessage(2L, 10L, SendChatMessageRequest("안녕하세요", mentionedUserIds = setOf(99L)))
            }

        assertEquals(ErrorCode.BAD_REQUEST, exception.errorCode)
        verify(messageRepository, org.mockito.Mockito.never()).saveAndFlush(any(ChatMessage::class.java))
    }

    @Test
    fun `투표는 익명이 기본이고 두 개부터 다섯 개 선택지를 저장한다`() {
        val member = user(2L)
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER)
        val savedMessage = mock(ChatMessage::class.java)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenReturn(savedMessage)
        `when`(savedMessage.id).thenReturn(30L)
        `when`(savedMessage.type).thenReturn(ChatMessageType.POLL)
        `when`(savedMessage.sender).thenReturn(member)
        `when`(savedMessage.content).thenReturn("어디서 만날까요?")
        `when`(savedMessage.pollAnonymous).thenReturn(true)
        `when`(savedMessage.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(pollOptionRepository.findAllByMessageIdOrderBySequenceAsc(30L)).thenReturn(emptyList())
        `when`(pollVoteRepository.findAllByMessageId(30L)).thenReturn(emptyList())
        val messageCaptor = ArgumentCaptor.forClass(ChatMessage::class.java)

        service.createPoll(
            2L,
            10L,
            CreateChatPollRequest(question = "어디서 만날까요?", options = listOf("서울역", "용산역")),
        )

        verify(messageRepository).saveAndFlush(messageCaptor.capture())
        assertEquals(ChatMessageType.POLL, messageCaptor.value.type)
        assertEquals(true, messageCaptor.value.pollAnonymous)
        verify(pollOptionRepository).saveAllAndFlush(org.mockito.Mockito.anyList<ChatPollOption>())
    }

    @Test
    fun `여행 당일 현재 시각을 기준으로 현재 장소와 다음 일정을 계산한다`() {
        val host = user(1L)
        val member = user(2L)
        val today = LocalDate.of(2026, 8, 21)
        val contentType = TourismContentType(12, "관광지")
        val course = TravelCourse(id = 5L, type = TravelCourseType.CUSTOM, owner = host, title = "서울 코스")
        course.addCustomPlace(TourismContent(contentId = 101L, contentType = contentType, title = "서울역"), 1, 1, LocalTime.of(9, 0))
        course.addCustomPlace(TourismContent(contentId = 102L, contentType = contentType, title = "남산"), 1, 2, LocalTime.of(11, 0))
        course.addCustomPlace(TourismContent(contentId = 103L, contentType = contentType, title = "한강"), 1, 3, LocalTime.of(14, 0))
        val room =
            room(
                host = host,
                course = course,
                startDate = today,
                endDate = today.plusDays(1),
                recruitmentDeadlineDate = today,
                status = ChatRoomStatus.CONFIRMED,
            )
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L))
            .thenReturn(ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER))

        val response = service.getCurrentRoadmap(2L, 10L, LocalDateTime.of(today, LocalTime.NOON))

        assertEquals(true, response.active)
        assertEquals(1, response.dayNumber)
        assertEquals("남산", response.currentPlace?.title)
        assertEquals(TravelRoadmapProgress.CURRENT, response.currentPlace?.progress)
        assertEquals("한강", response.nextPlace?.title)
        assertEquals(LocalTime.of(14, 0), response.nextPlace?.scheduledAt?.toLocalTime())
        assertEquals(
            listOf(TravelRoadmapProgress.COMPLETED, TravelRoadmapProgress.CURRENT, TravelRoadmapProgress.UPCOMING),
            response.places.map { it.progress },
        )
    }

    @Test
    fun `채팅방 참가자가 아니어도 채팅방 상세를 조회할 수 있다`() {
        val room = room(user(1L))
        val latestPinnedNotice = notice(7L, true, LocalDateTime.now(), room.host)
        `when`(roomRepository.findById(room.id)).thenReturn(Optional.of(room))
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)).thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndChatRoomId(2L, room.id)).thenReturn(true)
        `when`(
            noticeRepository.findFirstByChatRoomIdAndPinnedTrueAndContentIsNotNullOrderByCreatedDateTimeDescIdDesc(room.id),
        ).thenReturn(latestPinnedNotice)

        val response = service.getRoom(2L, room.id)

        assertEquals(room.id, response.roomId)
        assertEquals("https://cdn.example.com/chat-room.png", response.thumbnail)
        assertEquals(true, response.favorite)
        assertEquals(7L, response.latestPinnedNotice?.noticeId)
        verify(participantRepository).findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
    }

    @Test
    fun `공지 이력은 고정 여부로 나누고 저장소의 생성일 내림차순 순서를 유지한다`() {
        val room = room(user(1L))
        val now = LocalDateTime.now()
        val pinnedLatest = notice(3L, true, now, room.host)
        val unpinnedLatest = notice(2L, false, now.minusMinutes(1), room.host)
        val pinnedOld = notice(1L, true, now.minusMinutes(2), room.host)
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 2L)).thenReturn(true)
        `when`(noticeRepository.findAllByChatRoomIdAndContentIsNotNullOrderByCreatedDateTimeDescIdDesc(10L))
            .thenReturn(listOf(pinnedLatest, unpinnedLatest, pinnedOld))

        val response = service.getNoticeHistory(2L, 10L)

        assertEquals(listOf(3L, 1L), response.pinnedNotices.map { it.noticeId })
        assertEquals(listOf(2L), response.unpinnedNotices.map { it.noticeId })
    }

    @Test
    fun `고정 공지는 개수 제한 없이 추가할 수 있다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        service.createNotice(1L, 10L, "준비물 공지", pinned = true)

        verify(noticeRepository).save(any(ChatRoomNotice::class.java))
    }

    @Test
    fun `확정 채팅방 목록은 종료되지 않은 확정 방만 인원과 마감 정보를 포함해 반환한다`() {
        val user = user(2L)
        val confirmedRoom = room(user(1L), status = ChatRoomStatus.CONFIRMED)
        val endedRoom =
            room(
                user(3L),
                startDate = LocalDate.now().minusDays(2),
                endDate = LocalDate.now().minusDays(1),
                recruitmentDeadlineDate = LocalDate.now().minusDays(5),
                status = ChatRoomStatus.CONFIRMED,
            )
        val confirmedParticipant =
            ChatRoomParticipant(chatRoom = confirmedRoom, user = user, role = ChatParticipantRole.MEMBER)
        val endedParticipant = ChatRoomParticipant(chatRoom = endedRoom, user = user, role = ChatParticipantRole.MEMBER)
        val latestMessage = mock(ChatMessage::class.java)
        val now = LocalDateTime.now()
        `when`(latestMessage.type).thenReturn(ChatMessageType.SYSTEM)
        `when`(latestMessage.content).thenReturn("여행이 확정되었어요.")
        `when`(latestMessage.createdDateTime).thenReturn(now)
        `when`(participantRepository.findAllByUserId(2L)).thenReturn(listOf(confirmedParticipant, endedParticipant))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(3L)
        `when`(messageRepository.findFirstByChatRoomIdOrderByIdDesc(10L)).thenReturn(latestMessage)

        val response = service.getMyRooms(2L, MyChatRoomFilter.CONFIRMED)

        assertEquals(1, response.size)
        assertEquals(3, response.single().participantCount)
        assertEquals(3, response.single().maxParticipants)
        assertEquals(false, response.single().ended)
        assertEquals(5L, response.single().recruitmentDDay)
        assertEquals(now, response.single().latestMessage.sentAt)
    }

    @Test
    fun `동행자 목록은 프로필 정보와 여행 횟수 및 호스트와 본인 여부를 반환한다`() {
        val host = profiledUser(1L, Gender.F, LocalDate.now().minusYears(30))
        val me = profiledUser(2L, Gender.M, LocalDate.now().minusYears(28))
        val room = room(host)
        val hostParticipant = ChatRoomParticipant(chatRoom = room, user = host, role = ChatParticipantRole.HOST)
        val myParticipant = ChatRoomParticipant(chatRoom = room, user = me, role = ChatParticipantRole.MEMBER)
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 2L)).thenReturn(true)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(listOf(hostParticipant, myParticipant))
        `when`(participantRepository.countCompletedTrips(1L)).thenReturn(8L)
        `when`(participantRepository.countCompletedTrips(2L)).thenReturn(3L)
        `when`(applicationRepository.countByChatRoomIdAndStatus(10L, JoinApplicationStatus.WAITLISTED)).thenReturn(1L)

        val response = service.getMembers(2L, 10L)

        assertEquals(2, response.participantCount)
        assertEquals(3, response.maxParticipants)
        assertEquals(1, response.waitlistCount)
        assertEquals(true, response.members.first().host)
        assertEquals(false, response.members.first().me)
        assertEquals(8, response.members.first().completedTripCount)
        assertEquals(false, response.members.last().host)
        assertEquals(true, response.members.last().me)
        assertEquals(3, response.members.last().completedTripCount)
    }

    @Test
    fun `호스트가 멤버를 강퇴하면 사유를 비공개 이력에 저장한다`() {
        val host = user(1L)
        val member = user(2L)
        val room = room(host)
        val participant = ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }
        val kickHistoryCaptor = ArgumentCaptor.forClass(ChatRoomKickHistory::class.java)
        val messageCaptor = ArgumentCaptor.forClass(ChatMessage::class.java)

        service.kickMember(1L, 10L, 2L, "  반복적인 약속 불이행  ")

        verify(kickHistoryRepository).save(kickHistoryCaptor.capture())
        verify(messageRepository).saveAndFlush(messageCaptor.capture())
        assertEquals("반복적인 약속 불이행", kickHistoryCaptor.value.reason)
        assertEquals(false, messageCaptor.value.content.contains("약속 불이행"))
    }

    @Test
    fun `신청중 목록은 승인 대기와 승인 후 대기 상태만 조회한다`() {
        `when`(
            applicationRepository.findAllByUserIdAndStatusInOrderByCreatedDateTimeDesc(
                2L,
                listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED),
            ),
        ).thenReturn(emptyList())

        service.getMyWaitingRooms(2L)

        verify(applicationRepository).findAllByUserIdAndStatusInOrderByCreatedDateTimeDesc(
            2L,
            listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED),
        )
    }

    @Test
    fun `승인 대기중인 본인의 참가 신청을 방 아이디로 취소한다`() {
        val room = room(user(1L))
        val application =
            ChatRoomJoinApplication(
                id = 20L,
                chatRoom = room,
                user = user(2L),
                applicationMessage = "신청합니다",
                status = JoinApplicationStatus.PENDING,
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(
            applicationRepository.findFirstByChatRoomIdAndUserIdAndStatusInOrderByCreatedDateTimeDescIdDesc(
                10L,
                2L,
                listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED),
            ),
        ).thenReturn(application)

        service.cancelJoinApplication(2L, 10L)

        verify(applicationRepository).delete(application)
    }

    @Test
    fun `활성 참가 신청이 없으면 신청 취소에 실패한다`() {
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room(user(1L)))
        `when`(
            applicationRepository.findFirstByChatRoomIdAndUserIdAndStatusInOrderByCreatedDateTimeDescIdDesc(
                10L,
                2L,
                listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED),
            ),
        ).thenReturn(null)

        val exception =
            assertThrows(BaseException::class.java) {
                service.cancelJoinApplication(2L, 10L)
            }

        assertEquals(ErrorCode.CHAT_JOIN_APPLICATION_NOT_FOUND, exception.errorCode)
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
    fun `공개 코스 상세 조회는 만든 사람과 원본 여행 및 코스 정보를 반환한다`() {
        val creator = profiledUser(1L, Gender.F, LocalDate.of(1990, 1, 1))
        val course =
            TravelCourse(
                id = 5L,
                type = TravelCourseType.CUSTOM,
                owner = creator,
                title = "울릉도 대표 코스",
                description = "바다와 산을 함께 즐기는 코스",
                tripNights = 1,
                tripDays = 2,
            )
        course.addTags(listOf(TravelCourseTag(id = 7L, name = "힐링")))
        course.addCustomPlace(
            tourismContent =
                TourismContent(
                    contentId = 100L,
                    contentType = TourismContentType(12, "관광지"),
                    title = "주상절리",
                    thumbnail = "https://cdn.example.com/place.png",
                    latitude = 36.0,
                    longitude = 129.0,
                ),
            dayNumber = 1,
            sequence = 1,
            visitTime = LocalTime.of(10, 0),
        )
        course.publish()
        val creatorTravelRoom =
            room(
                host = creator,
                course = course,
                startDate = LocalDate.of(2026, 5, 25),
                endDate = LocalDate.of(2026, 5, 26),
                recruitmentDeadlineDate = LocalDate.of(2026, 5, 20),
                status = ChatRoomStatus.CONFIRMED,
            )
        `when`(courseRepository.findByIdAndType(5L, TravelCourseType.PUBLIC)).thenReturn(course)
        `when`(
            roomRepository.findFirstByCourseIdAndHostIdAndStatusOrderByStartDateAsc(
                5L,
                1L,
                ChatRoomStatus.CONFIRMED,
            ),
        ).thenReturn(creatorTravelRoom)
        `when`(roomRepository.countByCourseIdAndStatusNot(5L, ChatRoomStatus.CANCELLED)).thenReturn(3L)
        `when`(ratingRepository.findAverageByCourseId(5L)).thenReturn(4.46)
        `when`(ratingRepository.countByCourseId(5L)).thenReturn(12L)

        val response = service.getCourse(5L)

        assertEquals("울릉도 대표 코스", response.title)
        assertEquals("바다와 산을 함께 즐기는 코스", response.description)
        assertEquals("여행자1", response.creatorNickname)
        assertEquals(LocalDate.of(2026, 5, 25), response.creatorTravelStartDate)
        assertEquals(LocalDate.of(2026, 5, 26), response.creatorTravelEndDate)
        assertEquals(3L, response.chatRoomCount)
        assertEquals("1박 2일", response.travelTime)
        assertEquals(4.5, response.averageRating)
        assertEquals(12L, response.ratingCount)
        assertEquals(listOf("힐링"), response.tags.map { it.name })
        assertEquals(1, response.places.single().sequence)
        assertEquals(36.0, response.places.single().latitude)
        assertEquals(129.0, response.places.single().longitude)
        `when`(ratingRepository.findAverageByCourseId(5L)).thenReturn(4.04)
        assertEquals(4.0, service.getCourse(5L).averageRating)
    }

    @Test
    fun `태그를 선택하면 해당 태그의 공개 코스만 조회한다`() {
        val course = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "힐링 코스")
        `when`(courseRepository.findAllByTypeAndTagIdOrderByCreatedDateTimeDesc(TravelCourseType.PUBLIC, 7L))
            .thenReturn(listOf(course))
        `when`(ratingRepository.countByCourseId(5L)).thenReturn(0L)

        val response = service.getPublicCourses(tagId = 7L)

        assertEquals(listOf(5L), response.map { it.courseId })
        verify(courseRepository).findAllByTypeAndTagIdOrderByCreatedDateTimeDesc(TravelCourseType.PUBLIC, 7L)
    }

    @Test
    fun `태그를 선택하지 않으면 전체 공개 코스를 조회한다`() {
        `when`(courseRepository.findAllByTypeOrderByCreatedDateTimeDesc(TravelCourseType.PUBLIC)).thenReturn(emptyList())

        val response = service.getPublicCourses()

        assertEquals(emptyList<Any>(), response)
        verify(courseRepository).findAllByTypeOrderByCreatedDateTimeDesc(TravelCourseType.PUBLIC)
    }

    @Test
    fun `찜하지 않은 채팅방을 토글하면 찜 관계를 저장하고 true를 반환한다`() {
        val user = user(2L)
        val room = room(user(1L))
        `when`(favoriteRepository.findByUserIdAndChatRoomId(2L, 10L)).thenReturn(null)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(user))

        val response = service.toggleRoomFavorite(2L, 10L)

        assertEquals(true, response.favorite)
        verify(favoriteRepository).save(any(ChatRoomFavorite::class.java))
    }

    @Test
    fun `찜한 채팅방을 토글하면 찜 관계를 삭제하고 false를 반환한다`() {
        val room = room(user(1L))
        val favorite = ChatRoomFavorite(user = user(2L), chatRoom = room)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(favoriteRepository.findByUserIdAndChatRoomId(2L, 10L)).thenReturn(favorite)

        val response = service.toggleRoomFavorite(2L, 10L)

        assertEquals(false, response.favorite)
        verify(favoriteRepository).delete(favorite)
        verifyNoInteractions(userRepository)
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
    fun `자동 승인 방은 조건에 맞는 신청자를 즉시 참가시킨다`() {
        val host = user(1L)
        val applicant = user(2L)
        val room = room(host, joinApprovalMode = JoinApprovalMode.AUTO)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(1L)
        `when`(participantRepository.saveAndFlush(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        val response = service.applyToJoin(2L, 10L, JoinChatRoomRequest("함께 가고 싶어요"))

        assertEquals("JOINED", response.result.name)
        verify(participantRepository).saveAndFlush(any(ChatRoomParticipant::class.java))
    }

    @Test
    fun `참가 신청자가 성별 조건을 충족하지 않으면 거절한다`() {
        val host = user(1L)
        val applicant = profiledUser(2L, Gender.M, LocalDate.now().minusYears(30))
        val room = room(host, genderRestriction = GenderRestriction.FEMALE_ONLY)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val exception =
            assertThrows(BaseException::class.java) {
                service.applyToJoin(2L, 10L, JoinChatRoomRequest("함께 가고 싶어요"))
            }

        assertEquals(ErrorCode.CHAT_ROOM_JOIN_CONDITION_NOT_MET, exception.errorCode)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `참가 가능 여부 조회는 성별 제한을 충족하지 않으면 false를 반환한다`() {
        val applicant = profiledUser(2L, Gender.M, LocalDate.now().minusYears(30))
        val room = room(user(1L), genderRestriction = GenderRestriction.FEMALE_ONLY)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val response = service.getJoinEligibility(2L, 10L)

        assertEquals(false, response.canApply)
    }

    @Test
    fun `참가 가능 여부 조회는 연령 조건을 충족하면 신청 가능을 반환한다`() {
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L), minimumAge = 25, maximumAge = 35)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val response = service.getJoinEligibility(2L, 10L)

        assertEquals(true, response.canApply)
    }

    @Test
    fun `참가 가능 여부 조회는 연령 제한을 충족하지 않으면 false를 반환한다`() {
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L), maximumAge = 25)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val response = service.getJoinEligibility(2L, 10L)

        assertEquals(false, response.canApply)
    }

    @Test
    fun `최소 나이만 설정된 방은 최소 나이 이상인 신청자를 허용한다`() {
        val host = user(1L)
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(host, minimumAge = 25, joinApprovalMode = JoinApprovalMode.AUTO)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(1L)
        `when`(participantRepository.saveAndFlush(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        val response = service.applyToJoin(2L, 10L, JoinChatRoomRequest("함께 가고 싶어요"))

        assertEquals("JOINED", response.result.name)
    }

    @Test
    fun `최대 나이만 설정된 방은 최대 나이를 초과한 신청자를 거절한다`() {
        val host = user(1L)
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(host, maximumAge = 25)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val exception =
            assertThrows(BaseException::class.java) {
                service.applyToJoin(2L, 10L, JoinChatRoomRequest("함께 가고 싶어요"))
            }

        assertEquals(ErrorCode.CHAT_ROOM_JOIN_CONDITION_NOT_MET, exception.errorCode)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `수동 승인 방은 호스트에게 전할 말이 필수다`() {
        val host = user(1L)
        val applicant = user(2L)
        val room = room(host)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val exception =
            assertThrows(BaseException::class.java) {
                service.applyToJoin(2L, 10L, JoinChatRoomRequest())
            }

        assertEquals(ErrorCode.CHAT_JOIN_APPLICATION_MESSAGE_REQUIRED, exception.errorCode)
        verifyNoInteractions(messageRepository)
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

    private fun notice(
        id: Long,
        pinned: Boolean,
        createdAt: LocalDateTime,
        author: User,
    ): ChatRoomNotice =
        mock(ChatRoomNotice::class.java).also {
            `when`(it.id).thenReturn(id)
            `when`(it.content).thenReturn("공지 $id")
            `when`(it.pinned).thenReturn(pinned)
            `when`(it.author).thenReturn(author)
            `when`(it.createdDateTime).thenReturn(createdAt)
        }

    private fun profiledUser(
        id: Long,
        gender: Gender,
        birthDate: LocalDate,
    ) = User(
        id = id,
        userRole = UserRole.ROLE_USER,
        userInformation =
            UserInformation(
                nickname = "여행자$id",
                nicknameColor = NicknameColor.GREEN,
                gender = gender,
                birthDate = birthDate,
            ),
    )

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
        genderRestriction = GenderRestriction.NONE,
        joinApprovalMode = JoinApprovalMode.MANUAL,
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
        genderRestriction: GenderRestriction = GenderRestriction.NONE,
        minimumAge: Int? = null,
        maximumAge: Int? = null,
        joinApprovalMode: JoinApprovalMode = JoinApprovalMode.MANUAL,
        startDate: LocalDate = LocalDate.now().plusDays(10),
        endDate: LocalDate? = LocalDate.now().plusDays(11),
        recruitmentDeadlineDate: LocalDate = LocalDate.now().plusDays(5),
        status: ChatRoomStatus = ChatRoomStatus.RECRUITING,
        thumbnail: String? = "https://cdn.example.com/chat-room.png",
    ) = ChatRoom(
        id = 10L,
        host = host,
        course = course,
        roomTitle = "울릉도 여행",
        thumbnail = thumbnail,
        maxParticipants = 3,
        startDate = startDate,
        endDate = endDate,
        recruitmentDeadlineDate = recruitmentDeadlineDate,
        meetingLatitude = 36.0322,
        meetingLongitude = 129.3747,
        meetingDateTime = startDate.atStartOfDay(),
        participationFee = 100000L,
        genderRestriction = genderRestriction,
        minimumAge = minimumAge,
        maximumAge = maximumAge,
        joinApprovalMode = joinApprovalMode,
        status = status,
    )
}
