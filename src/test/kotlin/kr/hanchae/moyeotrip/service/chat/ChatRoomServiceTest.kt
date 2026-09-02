package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.controller.chat.request.CreateChatPollRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateCustomCourseRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateSettlementMemoRequest
import kr.hanchae.moyeotrip.controller.chat.request.CustomCoursePlaceRequest
import kr.hanchae.moyeotrip.controller.chat.request.JoinChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.MyChatRoomFilter
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.controller.chat.request.ShareTourismContentRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateMeetingInfoRequest
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollUpdatedOptionResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollUpdatedResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelRoadmapProgress
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatPollOption
import kr.hanchae.moyeotrip.entity.chat.ChatPollVote
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
import kr.hanchae.moyeotrip.repository.UserBlockRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import kr.hanchae.moyeotrip.utils.FhdWebpImageOptimizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream
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
    private val userBlockRepository = mock(UserBlockRepository::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val fhdWebpImageOptimizer = mock(FhdWebpImageOptimizer::class.java)
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
            userBlockRepository,
            objectStorageRepository,
            fhdWebpImageOptimizer,
            noticeRepository,
            notificationService,
            realtimeMessagingService,
        )

    @Test
    fun `모임 찾기는 차단 관계인 사용자가 속한 모임을 저장소 조회에서 제외한다`() {
        `when`(userBlockRepository.findRelatedUserIds(1L)).thenReturn(listOf(2L, 3L))
        `when`(
            roomRepository.searchRooms(
                1L,
                listOf(2L, 3L),
                "경주",
                LocalDate.now(),
                org.springframework.data.domain.PageRequest
                    .of(0, 20),
            ),
        ).thenReturn(emptyList())

        val response = service.searchRooms(1L, " 경주 ", 20)

        assertEquals(emptyList<Any>(), response)
        verify(roomRepository).searchRooms(
            1L,
            listOf(2L, 3L),
            "경주",
            LocalDate.now(),
            org.springframework.data.domain.PageRequest
                .of(0, 20),
        )
    }

    @Test
    fun `지도 탐색은 반경 내 집합 장소를 가까운 순서 응답으로 변환한다`() {
        val room = room(user(1L), meetingDetails = "포항역 1번 출구")
        val latitude = 36.0322
        val longitude = 129.3747
        val radiusKm = 5.0
        val latitudeDelta = Math.toDegrees(radiusKm / 6371.0088)
        val angularDistance = radiusKm / 6371.0088
        val longitudeDelta =
            Math.toDegrees(
                kotlin.math.asin(kotlin.math.sin(angularDistance) / kotlin.math.cos(Math.toRadians(latitude))),
            )
        val minimumLongitude = ((longitude - longitudeDelta + 540.0) % 360.0) - 180.0
        val maximumLongitude = ((longitude + longitudeDelta + 540.0) % 360.0) - 180.0
        `when`(userBlockRepository.findRelatedUserIds(7L)).thenReturn(emptyList())
        `when`(
            roomRepository.findMapRooms(
                userId = 7L,
                blockedUserIds = listOf(-1L),
                today = LocalDate.now(),
                minimumLatitude = latitude - latitudeDelta,
                maximumLatitude = latitude + latitudeDelta,
                minimumLongitude = minimumLongitude,
                maximumLongitude = maximumLongitude,
                crossesDateLine = false,
            ),
        ).thenReturn(listOf(room))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(2L)
        `when`(favoriteRepository.findChatRoomIdsByUserIdAndChatRoomIdIn(7L, listOf(10L))).thenReturn(setOf(10L))

        val response = service.getMapRooms(7L, latitude, longitude, radiusKm).single()

        assertEquals(10L, response.roomId)
        assertEquals(0L, response.distanceMeters)
        assertEquals(true, response.favorite)
        assertEquals("포항역 1번 출구", response.meetingDetails)
    }

    @Test
    fun `지도 탐색은 유효하지 않은 좌표와 반경을 거부한다`() {
        val invalidAreas =
            listOf(
                Triple(Double.NaN, 127.0, 1.0),
                Triple(Double.POSITIVE_INFINITY, 127.0, 1.0),
                Triple(91.0, 127.0, 1.0),
                Triple(36.0, Double.NaN, 1.0),
                Triple(36.0, Double.NEGATIVE_INFINITY, 1.0),
                Triple(36.0, 181.0, 1.0),
                Triple(36.0, 127.0, Double.NaN),
                Triple(36.0, 127.0, Double.POSITIVE_INFINITY),
                Triple(36.0, 127.0, 0.0),
            )

        invalidAreas.forEach { (latitude, longitude, radiusKm) ->
            val exception =
                assertThrows(BaseException::class.java) {
                    service.getMapRooms(1L, latitude, longitude, radiusKm)
                }
            assertEquals(ErrorCode.INVALID_MAP_SEARCH_AREA, exception.errorCode)
        }
        verifyNoInteractions(userBlockRepository, roomRepository)
    }

    @Test
    fun `지도 탐색은 경북 서비스 범위를 넘는 반경을 거부한다`() {
        val exception = assertThrows(BaseException::class.java) { service.getMapRooms(1L, 36.4, 128.9, 200.1) }

        assertEquals(ErrorCode.INVALID_MAP_SEARCH_AREA, exception.errorCode)
        verifyNoInteractions(userBlockRepository, roomRepository)
    }

    @Test
    fun `지도 탐색 결과가 없으면 찜 저장소를 조회하지 않는다`() {
        val latitude = 36.0
        val longitude = 128.0
        val radiusKm = 5.0
        val angularDistance = radiusKm / 6371.0088
        val latitudeDelta = Math.toDegrees(angularDistance)
        val longitudeDelta =
            Math.toDegrees(
                kotlin.math.asin(kotlin.math.sin(angularDistance) / kotlin.math.cos(Math.toRadians(latitude))),
            )
        `when`(userBlockRepository.findRelatedUserIds(7L)).thenReturn(listOf(9L))
        `when`(
            roomRepository.findMapRooms(
                userId = 7L,
                blockedUserIds = listOf(9L),
                today = LocalDate.now(),
                minimumLatitude = latitude - latitudeDelta,
                maximumLatitude = latitude + latitudeDelta,
                minimumLongitude = ((longitude - longitudeDelta + 540.0) % 360.0) - 180.0,
                maximumLongitude = ((longitude + longitudeDelta + 540.0) % 360.0) - 180.0,
                crossesDateLine = false,
            ),
        ).thenReturn(emptyList())

        assertTrue(service.getMapRooms(7L, latitude, longitude, radiusKm).isEmpty())

        verifyNoInteractions(favoriteRepository)
    }

    @Test
    fun `극지방 지도 탐색은 전체 경도 범위를 사용하고 좌표가 없거나 반경 밖인 방을 제외한다`() {
        val latitude = 89.9
        val longitude = 179.9
        val radiusKm = 200.0
        val latitudeDelta = Math.toDegrees(radiusKm / 6371.0088)
        val missingCoordinates = room(user(1L), meetingLatitude = null, meetingLongitude = null)
        val outsideRadius = room(user(2L), meetingLatitude = 0.0, meetingLongitude = 0.0)
        `when`(userBlockRepository.findRelatedUserIds(7L)).thenReturn(emptyList())
        `when`(
            roomRepository.findMapRooms(
                userId = 7L,
                blockedUserIds = listOf(-1L),
                today = LocalDate.now(),
                minimumLatitude = latitude - latitudeDelta,
                maximumLatitude = 90.0,
                minimumLongitude = -180.0,
                maximumLongitude = 180.0,
                crossesDateLine = false,
            ),
        ).thenReturn(listOf(missingCoordinates, outsideRadius))

        assertTrue(service.getMapRooms(7L, latitude, longitude, radiusKm).isEmpty())

        verifyNoInteractions(favoriteRepository)
    }

    @Test
    fun `모임 찾기는 빈 검색어와 제한값을 정규화하고 방 카드 정보를 반환한다`() {
        val course = TravelCourse(id = 5L, type = TravelCourseType.CUSTOM, title = "경주 코스")
        course.addTags(listOf(TravelCourseTag(id = 2L, name = "역사"), TravelCourseTag(id = 1L, name = "힐링")))
        course.publish()
        val room = room(user(1L), course = course)
        `when`(userBlockRepository.findRelatedUserIds(2L)).thenReturn(emptyList())
        `when`(
            roomRepository.searchRooms(
                2L,
                listOf(-1L),
                null,
                LocalDate.now(),
                PageRequest.of(0, 20),
            ),
        ).thenReturn(listOf(room))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(2L)

        val response = service.searchRooms(2L, "   ", 100)

        assertEquals(1, response.size)
        assertEquals(listOf(1L, 2L), response.single().tags.map { it.tagId })
        assertEquals(2, response.single().participantCount)
    }

    @Test
    fun `공개 코스로 만든 모집 방 목록은 공개 코스를 검증하고 검색 조건을 적용한다`() {
        val course = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "주왕산 대표 코스")
        val room = room(user(2L), course = course)
        `when`(courseRepository.findByIdAndType(5L, TravelCourseType.PUBLIC)).thenReturn(course)
        `when`(userBlockRepository.findRelatedUserIds(1L)).thenReturn(emptyList())
        `when`(
            roomRepository.findRecruitingRoomsByPublicCourseId(
                1L,
                listOf(-1L),
                5L,
                LocalDate.now(),
                PageRequest.of(0, 20),
            ),
        ).thenReturn(listOf(room))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(1L)

        val response = service.getPublicCourseChatRooms(1L, 5L, 100)

        assertEquals(listOf(10L), response.map { it.roomId })
        verify(roomRepository).findRecruitingRoomsByPublicCourseId(
            1L,
            listOf(-1L),
            5L,
            LocalDate.now(),
            PageRequest.of(0, 20),
        )
    }

    @Test
    fun `모임 찾기는 상태와 찜 여부 및 인원 정보를 반환한다`() {
        val startDate = LocalDate.now().plusDays(10)
        val room =
            room(
                user(1L),
                startDate = startDate,
                endDate = null,
                dayTripStartTime = LocalTime.of(9, 0),
                dayTripEndTime = LocalTime.of(18, 0),
                recruitmentDeadlineDate = LocalDate.now().plusDays(5),
                meetingDetails = "포항역 1번 출구",
            )
        `when`(userBlockRepository.findRelatedUserIds(7L)).thenReturn(emptyList())
        `when`(
            roomRepository.searchRooms(
                7L,
                listOf(-1L),
                null,
                LocalDate.now(),
                org.springframework.data.domain.PageRequest
                    .of(0, 20),
            ),
        ).thenReturn(listOf(room))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(2L)
        `when`(favoriteRepository.findChatRoomIdsByUserIdAndChatRoomIdIn(7L, listOf(10L))).thenReturn(setOf(10L))

        val response = service.searchRooms(7L, null, 20).single()

        assertEquals(ChatRoomStatus.RECRUITING, response.status)
        assertEquals(true, response.favorite)
        assertEquals(2, response.participantCount)
        assertEquals(3, response.maxParticipants)
    }

    @Test
    fun `모집 마감일이 지난 채팅방의 D day는 null이다`() {
        val room = room(user(1L), recruitmentDeadlineDate = LocalDate.now().minusDays(1))

        assertEquals(null, room.recruitmentDDay())
    }

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

        assertEquals(ErrorCode.INVALID_CHAT_IMAGE, exception.errorCode)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `비어 있는 파일은 채팅 사진으로 공유할 수 없다`() {
        val image = mock(MultipartFile::class.java)
        `when`(image.isEmpty).thenReturn(true)

        val exception = assertThrows(BaseException::class.java) { service.shareImage(2L, 10L, image, null) }

        assertEquals(ErrorCode.INVALID_CHAT_IMAGE, exception.errorCode)
        verifyNoInteractions(participantRepository, messageRepository)
    }

    @Test
    fun `이미지 형식이 아닌 파일은 채팅 사진으로 공유할 수 없다`() {
        val image = mock(MultipartFile::class.java)
        `when`(image.isEmpty).thenReturn(false)
        `when`(image.size).thenReturn(1024L)
        `when`(image.contentType).thenReturn("text/plain")

        val exception = assertThrows(BaseException::class.java) { service.shareImage(2L, 10L, image, null) }

        assertEquals(ErrorCode.INVALID_CHAT_IMAGE, exception.errorCode)
        verifyNoInteractions(participantRepository, messageRepository)
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

        assertEquals(ErrorCode.MENTIONED_USER_NOT_PARTICIPANT, exception.errorCode)
        verify(messageRepository, org.mockito.Mockito.never()).saveAndFlush(any(ChatMessage::class.java))
    }

    @Test
    fun `채팅 메시지는 공백을 정리하고 멘션과 읽음 상태를 저장한 뒤 실시간 전파한다`() {
        val sender = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val mentioned = profiledUser(3L, Gender.M, LocalDate.now().minusYears(31))
        val room = room(user(1L))
        val senderParticipant = ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.MEMBER)
        val mentionedParticipant = ChatRoomParticipant(chatRoom = room, user = mentioned, role = ChatParticipantRole.MEMBER)
        val saved = message(30L, room, sender, content = "안녕하세요")
        `when`(saved.mentionedUsers).thenReturn(setOf(mentioned))
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(senderParticipant)
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(listOf(senderParticipant, mentionedParticipant))
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenReturn(saved)

        val response =
            service.sendMessage(
                2L,
                10L,
                SendChatMessageRequest("  안녕하세요  ", mentionedUserIds = setOf(3L)),
            )

        val captor = ArgumentCaptor.forClass(ChatMessage::class.java)
        verify(messageRepository).saveAndFlush(captor.capture())
        assertEquals("안녕하세요", captor.value.content)
        assertEquals(setOf(mentioned), captor.value.mentionedUsers)
        assertEquals(30L, senderParticipant.lastReadMessageId)
        assertEquals(30L, response.messageId)
        verify(notificationService).notifyMessage(saved)
        verify(realtimeMessagingService).sendChatMessage(10L, response)
    }

    @Test
    fun `관광 콘텐츠를 채팅방에 공유하면 콘텐츠 카드 메시지를 저장한다`() {
        val sender = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.MEMBER)
        val content =
            TourismContent(
                contentId = 100L,
                contentType = TourismContentType(12, "관광지"),
                title = "불국사",
                latitude = 35.7900,
                longitude = 129.3320,
            )
        val saved = message(31L, room, sender, ChatMessageType.TOURISM_CONTENT, "불국사")
        `when`(saved.tourismContent).thenReturn(content)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(tourismContentRepository.findByContentId(100L)).thenReturn(content)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenReturn(saved)

        val response = service.shareTourismContent(2L, 10L, ShareTourismContentRequest(100L))

        assertEquals(ChatMessageType.TOURISM_CONTENT, response.type)
        assertEquals(100L, response.tourismContent?.contentId)
        verify(realtimeMessagingService).sendChatMessage(10L, response)
    }

    @Test
    fun `유효한 채팅 이미지는 저장소에 업로드하고 사진 메시지로 공유한다`() {
        val sender = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.MEMBER)
        val image = mock(MultipartFile::class.java)
        val stream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val saved = message(32L, room, sender, ChatMessageType.IMAGE, "여행 사진")
        `when`(saved.imageUrl).thenReturn("https://cdn.example.com/chat/image.png")
        `when`(image.isEmpty).thenReturn(false)
        `when`(image.size).thenReturn(3L)
        `when`(image.contentType).thenReturn("image/png")
        `when`(image.originalFilename).thenReturn("TRIP.PNG")
        `when`(image.inputStream).thenReturn(stream)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(
            objectStorageRepository.upload(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                anyInputStream(),
            ),
        ).thenReturn("chat/message/image/generated.png")
        `when`(objectStorageRepository.getDownloadUrl("chat/message/image/generated.png"))
            .thenReturn("https://cdn.example.com/chat/image.png")
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenReturn(saved)

        val response = service.shareImage(2L, 10L, image, "  여행 사진  ")

        assertEquals(ChatMessageType.IMAGE, response.type)
        assertEquals("https://cdn.example.com/chat/image.png", response.imageUrl)
        val captor = ArgumentCaptor.forClass(ChatMessage::class.java)
        verify(messageRepository).saveAndFlush(captor.capture())
        assertEquals("여행 사진", captor.value.content)
    }

    @Test
    fun `정산 메모는 공백을 정리해 카드 메시지로 공유한다`() {
        val sender = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.MEMBER)
        val saved = message(33L, room, sender, ChatMessageType.SETTLEMENT_MEMO, "1인 9000원")
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenReturn(saved)

        val response = service.shareSettlementMemo(2L, 10L, CreateSettlementMemoRequest("  1인 9000원  "))

        assertEquals(ChatMessageType.SETTLEMENT_MEMO, response.type)
        val captor = ArgumentCaptor.forClass(ChatMessage::class.java)
        verify(messageRepository).saveAndFlush(captor.capture())
        assertEquals("1인 9000원", captor.value.content)
    }

    @Test
    fun `취소된 채팅방에는 새 메시지를 보낼 수 없다`() {
        val sender = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val closedRoom = room(user(1L), status = ChatRoomStatus.CANCELLED)
        val participant = ChatRoomParticipant(chatRoom = closedRoom, user = sender, role = ChatParticipantRole.MEMBER)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)

        val exception =
            assertThrows(BaseException::class.java) {
                service.sendMessage(2L, 10L, SendChatMessageRequest("아직 보낼 수 있나요?"))
            }

        assertEquals(ErrorCode.CHAT_DISABLED, exception.errorCode)
        verifyNoInteractions(messageRepository, notificationService, realtimeMessagingService)
    }

    @Test
    fun `존재하지 않거나 다른 방의 메시지에는 답글을 보낼 수 없다`() {
        val sender = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.MEMBER)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.findByIdAndChatRoomId(999L, 10L)).thenReturn(null)

        val exception =
            assertThrows(BaseException::class.java) {
                service.sendMessage(2L, 10L, SendChatMessageRequest("답글", replyToMessageId = 999L))
            }

        assertEquals(ErrorCode.CHAT_REPLY_MESSAGE_NOT_FOUND, exception.errorCode)
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
    fun `공백을 제거한 투표 선택지가 중복되면 투표를 만들 수 없다`() {
        val exception =
            assertThrows(BaseException::class.java) {
                service.createPoll(
                    2L,
                    10L,
                    CreateChatPollRequest(question = "점심 메뉴", options = listOf("한식", " 한식 ")),
                )
            }

        assertEquals(ErrorCode.DUPLICATE_CHAT_POLL_OPTION, exception.errorCode)
        verifyNoInteractions(participantRepository, messageRepository, pollOptionRepository)
    }

    @Test
    fun `투표 선택을 바꾸면 기존 투표 행의 선택지만 변경한다`() {
        val voter = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = voter, role = ChatParticipantRole.MEMBER)
        val pollMessage = message(30L, room, user(1L), ChatMessageType.POLL, "어디서 만날까요?")
        `when`(pollMessage.pollAnonymous).thenReturn(true)
        val oldOption = ChatPollOption(id = 40L, message = pollMessage, text = "서울역", sequence = 1)
        val newOption = ChatPollOption(id = 41L, message = pollMessage, text = "용산역", sequence = 2)
        val oldVote = ChatPollVote(id = 50L, message = pollMessage, option = oldOption, user = voter)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(pollMessage)
        `when`(pollOptionRepository.findByIdAndMessageId(41L, 30L)).thenReturn(newOption)
        `when`(pollVoteRepository.findByMessageIdAndUserId(30L, 2L)).thenReturn(oldVote)
        `when`(pollOptionRepository.findAllByMessageIdOrderBySequenceAsc(30L)).thenReturn(listOf(oldOption, newOption))
        `when`(pollVoteRepository.findAllByMessageId(30L)).thenReturn(emptyList())

        service.votePoll(2L, 10L, 30L, 41L)

        verify(pollVoteRepository).flush()
        verify(pollVoteRepository, never()).delete(oldVote)
        verify(pollVoteRepository, never()).saveAndFlush(any(ChatPollVote::class.java))
        assertEquals(41L, oldVote.option.id)
        verify(realtimeMessagingService).sendChatPollUpdated(
            10L,
            ChatPollUpdatedResponse(
                30L,
                0,
                listOf(
                    ChatPollUpdatedOptionResponse(40L, 0, null),
                    ChatPollUpdatedOptionResponse(41L, 0, null),
                ),
            ),
        )
    }

    @Test
    fun `처음 투표하면 한 사람당 하나의 투표를 저장한다`() {
        val voter = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = voter, role = ChatParticipantRole.MEMBER)
        val pollMessage = message(30L, room, user(1L), ChatMessageType.POLL, "어디서 만날까요?")
        `when`(pollMessage.pollAnonymous).thenReturn(true)
        val option = ChatPollOption(id = 40L, message = pollMessage, text = "서울역", sequence = 1)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(pollMessage)
        `when`(pollOptionRepository.findByIdAndMessageId(40L, 30L)).thenReturn(option)
        `when`(pollOptionRepository.findAllByMessageIdOrderBySequenceAsc(30L)).thenReturn(listOf(option))
        `when`(pollVoteRepository.findAllByMessageId(30L)).thenReturn(emptyList())

        service.votePoll(2L, 10L, 30L, 40L)

        val captor = ArgumentCaptor.forClass(ChatPollVote::class.java)
        verify(pollVoteRepository).saveAndFlush(captor.capture())
        assertEquals(40L, captor.value.option.id)
        assertEquals(2L, captor.value.user.id)
    }

    @Test
    fun `같은 선택지에 다시 투표하면 저장소를 변경하지 않는다`() {
        val voter = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = voter, role = ChatParticipantRole.MEMBER)
        val pollMessage = message(30L, room, user(1L), ChatMessageType.POLL, "어디서 만날까요?")
        `when`(pollMessage.pollAnonymous).thenReturn(true)
        val option = ChatPollOption(id = 40L, message = pollMessage, text = "서울역", sequence = 1)
        val vote = ChatPollVote(id = 50L, message = pollMessage, option = option, user = voter)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(pollMessage)
        `when`(pollOptionRepository.findByIdAndMessageId(40L, 30L)).thenReturn(option)
        `when`(pollVoteRepository.findByMessageIdAndUserId(30L, 2L)).thenReturn(vote)
        `when`(pollOptionRepository.findAllByMessageIdOrderBySequenceAsc(30L)).thenReturn(listOf(option))
        `when`(pollVoteRepository.findAllByMessageId(30L)).thenReturn(listOf(vote))

        val response = service.votePoll(2L, 10L, 30L, 40L)

        verify(pollVoteRepository, never()).flush()
        verify(pollVoteRepository, never()).saveAndFlush(any(ChatPollVote::class.java))
        assertEquals(
            true,
            response.poll
                ?.options
                ?.single()
                ?.votedByMe,
        )
        assertEquals(1, response.poll?.totalVoteCount)
    }

    @Test
    fun `투표 취소는 내 표가 있을 때만 삭제하고 현재 투표 결과를 반환한다`() {
        val voter = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = voter, role = ChatParticipantRole.MEMBER)
        val pollMessage = message(30L, room, user(1L), ChatMessageType.POLL, "어디서 만날까요?")
        `when`(pollMessage.pollAnonymous).thenReturn(true)
        val option = ChatPollOption(id = 40L, message = pollMessage, text = "서울역", sequence = 1)
        val vote = ChatPollVote(id = 50L, message = pollMessage, option = option, user = voter)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(pollMessage)
        `when`(pollVoteRepository.findByMessageIdAndUserId(30L, 2L)).thenReturn(vote)
        `when`(pollOptionRepository.findAllByMessageIdOrderBySequenceAsc(30L)).thenReturn(listOf(option))
        `when`(pollVoteRepository.findAllByMessageId(30L)).thenReturn(emptyList())

        val response = service.cancelPollVote(2L, 10L, 30L)

        verify(pollVoteRepository).delete(vote)
        verify(pollVoteRepository).flush()
        assertEquals(0, response.poll?.totalVoteCount)
        verify(realtimeMessagingService).sendChatPollUpdated(
            10L,
            ChatPollUpdatedResponse(30L, 0, listOf(ChatPollUpdatedOptionResponse(40L, 0, null))),
        )
    }

    @Test
    fun `집합 위치 공유는 위도가 없으면 거부한다`() {
        val member = user(2L)
        val room = mock(ChatRoom::class.java)
        `when`(room.canChat()).thenReturn(true)
        `when`(room.meetingLatitude).thenReturn(null)
        `when`(room.meetingLongitude).thenReturn(129.3747)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L))
            .thenReturn(ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER))

        val exception = assertThrows(BaseException::class.java) { service.shareLocation(2L, 10L) }

        assertEquals(ErrorCode.CHAT_ROOM_MEETING_LOCATION_NOT_SET, exception.errorCode)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `집합 위치 공유는 경도가 없으면 거부한다`() {
        val member = user(2L)
        val room = mock(ChatRoom::class.java)
        `when`(room.canChat()).thenReturn(true)
        `when`(room.meetingLatitude).thenReturn(36.0322)
        `when`(room.meetingLongitude).thenReturn(null)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L))
            .thenReturn(ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER))

        val exception = assertThrows(BaseException::class.java) { service.shareLocation(2L, 10L) }

        assertEquals(ErrorCode.CHAT_ROOM_MEETING_LOCATION_NOT_SET, exception.errorCode)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `집합 장소명이 공백이면 기본 이름으로 위치를 공유한다`() {
        val member = user(2L)
        val room = room(user(1L), meetingDetails = "   ")
        val savedMessage = message(30L, room, member, ChatMessageType.LOCATION, "만날 위치")
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L))
            .thenReturn(ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER))
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenReturn(savedMessage)

        val response = service.shareLocation(2L, 10L)

        assertEquals("만날 위치", response.content)
        verify(realtimeMessagingService).sendChatMessage(10L, response)
    }

    @Test
    fun `커서가 없을 때 다음 페이지가 있으면 가장 오래된 반환 메시지 ID를 다음 커서로 준다`() {
        val member = user(2L)
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER)
        val messages =
            listOf(
                message(12L, room, member),
                message(11L, room, member),
                message(10L, room, member),
            )
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(messageRepository.findAllByChatRoomIdOrderByIdDesc(10L, PageRequest.of(0, 3))).thenReturn(messages)

        val response = service.getMessages(2L, 10L, beforeMessageId = null, limit = 2)

        assertEquals(listOf(11L, 12L), response.messages.map { it.messageId })
        assertEquals(11L, response.nextId)
        assertEquals(true, response.hasNext)
        assertEquals(12L, participant.lastReadMessageId)
    }

    @Test
    fun `커서 다음 메시지가 없으면 다음 커서를 반환하지 않는다`() {
        val member = user(2L)
        val room = room(user(1L))
        val participant = ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER)
        val messages = listOf(message(8L, room, member), message(7L, room, member))
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(
            messageRepository.findAllByChatRoomIdAndIdLessThanOrderByIdDesc(
                10L,
                9L,
                PageRequest.of(0, 3),
            ),
        ).thenReturn(messages)

        val response = service.getMessages(2L, 10L, beforeMessageId = 9L, limit = 2)

        assertEquals(listOf(7L, 8L), response.messages.map { it.messageId })
        assertEquals(null, response.nextId)
        assertEquals(false, response.hasNext)
    }

    @Test
    fun `여행 기간이 아니면 현재 로드맵은 비활성 상태다`() {
        val member = user(2L)
        val room = room(user(1L), status = ChatRoomStatus.CONFIRMED)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L))
            .thenReturn(ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER))

        val response = service.getCurrentRoadmap(2L, 10L, LocalDateTime.now())

        assertEquals(false, response.active)
        assertEquals(emptyList<Any>(), response.places)
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
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)))
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
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
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
        val savedNotice = ChatRoomNotice(id = 7L, chatRoom = room, author = room.host, content = "준비물 공지", pinned = true)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(noticeRepository.save(any(ChatRoomNotice::class.java))).thenReturn(savedNotice)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        val noticeId = service.createNotice(1L, 10L, "준비물 공지", pinned = true)

        assertEquals(7L, noticeId)
        verify(noticeRepository).save(any(ChatRoomNotice::class.java))
    }

    @Test
    fun `호스트는 기존 공지의 내용과 고정 여부를 수정할 수 있다`() {
        val host = user(1L)
        val room = room(host)
        val notice = ChatRoomNotice(id = 7L, chatRoom = room, author = host, content = "기존 공지", pinned = false)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(noticeRepository.findByIdAndChatRoomId(7L, 10L)).thenReturn(notice)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        service.updateNotice(1L, 10L, 7L, "  수정된 공지  ", pinned = true)

        assertEquals("수정된 공지", notice.content)
        assertEquals(true, notice.pinned)
        val captor = ArgumentCaptor.forClass(ChatMessage::class.java)
        verify(messageRepository).saveAndFlush(captor.capture())
        assertEquals(true, captor.value.content.contains("수정된 공지"))
    }

    @Test
    fun `호스트는 기존 공지를 삭제할 수 있다`() {
        val host = user(1L)
        val room = room(host)
        val notice = ChatRoomNotice(id = 7L, chatRoom = room, author = host, content = "삭제할 공지", pinned = true)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(noticeRepository.findByIdAndChatRoomId(7L, 10L)).thenReturn(notice)

        service.deleteNotice(1L, 10L, 7L)

        verify(noticeRepository).delete(notice)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `빈 공지는 등록하거나 기존 공지의 내용으로 저장할 수 없다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)

        val createException =
            assertThrows(BaseException::class.java) {
                service.createNotice(1L, 10L, "   ", pinned = false)
            }
        val updateException =
            assertThrows(BaseException::class.java) {
                service.updateNotice(1L, 10L, 7L, "   ", pinned = null)
            }

        assertEquals(ErrorCode.NOTICE_CONTENT_BLANK, createException.errorCode)
        assertEquals(ErrorCode.NOTICE_CONTENT_BLANK, updateException.errorCode)
        verifyNoInteractions(noticeRepository)
    }

    @Test
    fun `존재하지 않는 공지는 수정할 수 없다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(noticeRepository.findByIdAndChatRoomId(7L, 10L)).thenReturn(null)

        val exception =
            assertThrows(BaseException::class.java) {
                service.updateNotice(1L, 10L, 7L, "새 공지", pinned = true)
            }

        assertEquals(ErrorCode.CHAT_ROOM_NOTICE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `확정 채팅방 목록은 종료되지 않은 확정 방의 채팅 요약을 반환한다`() {
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
        assertEquals(true, response.single().chatAvailable)
        assertEquals(5L, response.single().recruitmentDDay)
        assertEquals(now, response.single().latestMessage!!.sentAt)
    }

    @Test
    fun `지난 여행의 호스트이고 커스텀 코스가 미공개면 코스 공개 버튼을 표시한다`() {
        val host = user(1L)
        val course = TravelCourse(id = 5L, type = TravelCourseType.CUSTOM, owner = host, title = "직접 만든 코스")
        val endedRoom =
            room(
                host = host,
                course = course,
                startDate = LocalDate.now().minusDays(2),
                endDate = LocalDate.now().minusDays(1),
                recruitmentDeadlineDate = LocalDate.now().minusDays(3),
                status = ChatRoomStatus.CONFIRMED,
            )
        val participant = ChatRoomParticipant(chatRoom = endedRoom, user = host, role = ChatParticipantRole.HOST)
        val latestMessage = mock(ChatMessage::class.java)
        `when`(latestMessage.type).thenReturn(ChatMessageType.SYSTEM)
        `when`(latestMessage.content).thenReturn("여행이 끝났어요.")
        `when`(latestMessage.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(participantRepository.findAllByUserId(host.id)).thenReturn(listOf(participant))
        `when`(messageRepository.findFirstByChatRoomIdOrderByIdDesc(endedRoom.id)).thenReturn(latestMessage)

        val response = service.getMyRooms(host.id, MyChatRoomFilter.ENDED)

        assertEquals(true, response.single().coursePublicationAvailable)
    }

    @Test
    fun `삭제된 지난 여행은 채팅 정보 없이 기본 정보만 반환한다`() {
        val host = user(1L)
        val course = TravelCourse(id = 5L, type = TravelCourseType.CUSTOM, owner = host, title = "직접 만든 코스")
        val room =
            room(
                host = host,
                course = course,
                startDate = LocalDate.now().minusDays(20),
                endDate = LocalDate.now().minusDays(19),
                recruitmentDeadlineDate = LocalDate.now().minusDays(21),
                status = ChatRoomStatus.CONFIRMED,
            ).also { it.archiveChat(LocalDateTime.now()) }
        val participant = ChatRoomParticipant(chatRoom = room, user = host, role = ChatParticipantRole.HOST)
        `when`(participantRepository.findAllByUserId(host.id)).thenReturn(listOf(participant))

        val response = service.getMyRooms(host.id, MyChatRoomFilter.ENDED).single()

        assertEquals("울릉도 여행", response.title)
        assertEquals(null, response.description)
        assertEquals(room.startDate, response.startDate)
        assertEquals(room.endDate, response.endDate)
        assertEquals(false, response.chatAvailable)
        assertEquals(null, response.thumbnail)
        assertEquals(null, response.status)
        assertEquals(null, response.recruitmentDDay)
        assertEquals(null, response.participantCount)
        assertEquals(null, response.unreadMessageCount)
        assertEquals(null, response.latestMessage)
        assertEquals(true, response.coursePublicationAvailable)
    }

    @Test
    fun `메시지가 삭제된 지난 여행의 채팅방 상세는 조회할 수 없다`() {
        val room =
            room(
                host = user(1L),
                startDate = LocalDate.now().minusDays(20),
                endDate = LocalDate.now().minusDays(19),
                recruitmentDeadlineDate = LocalDate.now().minusDays(21),
                status = ChatRoomStatus.CONFIRMED,
            ).also { it.archiveChat(LocalDateTime.now()) }
        `when`(roomRepository.findById(room.id)).thenReturn(Optional.of(room))

        val exception = assertThrows(BaseException::class.java) { service.getRoom(1L, room.id) }

        assertEquals(ErrorCode.CHAT_ROOM_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(favoriteRepository)
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
        `when`(kickHistoryRepository.save(any(ChatRoomKickHistory::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }
        val kickHistoryCaptor = ArgumentCaptor.forClass(ChatRoomKickHistory::class.java)
        val messageCaptor = ArgumentCaptor.forClass(ChatMessage::class.java)

        service.kickMember(1L, 10L, 2L, "  반복적인 약속 불이행  ")

        verify(kickHistoryRepository).save(kickHistoryCaptor.capture())
        verify(notificationService).notifyChatRoomMemberKicked(kickHistoryCaptor.value)
        verify(messageRepository).saveAndFlush(messageCaptor.capture())
        assertEquals("반복적인 약속 불이행", kickHistoryCaptor.value.reason)
        assertEquals(false, messageCaptor.value.content.contains("약속 불이행"))
    }

    @Test
    fun `강퇴 사유가 공백이면 멤버를 삭제하지 않는다`() {
        val host = user(1L)
        val member = user(2L)
        val room = room(host)
        val participant = ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)

        val exception = assertThrows(BaseException::class.java) { service.kickMember(1L, 10L, 2L, "   ") }

        assertEquals(ErrorCode.KICK_REASON_BLANK, exception.errorCode)
        verify(participantRepository, org.mockito.Mockito.never()).delete(participant)
        verifyNoInteractions(kickHistoryRepository, notificationService, messageRepository)
    }

    @Test
    fun `호스트가 아닌 사용자는 멤버를 강퇴할 수 없다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)

        val exception = assertThrows(BaseException::class.java) { service.kickMember(2L, 10L, 3L, "사유") }

        assertEquals(ErrorCode.CHAT_ROOM_HOST_REQUIRED, exception.errorCode)
        verifyNoInteractions(participantRepository, kickHistoryRepository, notificationService, messageRepository)
    }

    @Test
    fun `강퇴된 사용자는 자신의 강퇴 사유 이력을 최신순 응답으로 조회한다`() {
        val history = mock(ChatRoomKickHistory::class.java)
        val kickedAt = LocalDateTime.of(2026, 8, 23, 10, 0)
        `when`(history.id).thenReturn(44L)
        `when`(history.chatRoomId).thenReturn(10L)
        `when`(history.roomTitle).thenReturn("경주 여행")
        `when`(history.reason).thenReturn("반복적인 약속 불이행")
        `when`(history.createdDateTime).thenReturn(kickedAt)
        `when`(kickHistoryRepository.findAllByKickedUserIdOrderByCreatedDateTimeDescIdDesc(2L)).thenReturn(listOf(history))

        val response = service.getMyKickHistories(2L)

        assertEquals(44L, response.single().kickHistoryId)
        assertEquals("반복적인 약속 불이행", response.single().reason)
        assertEquals(kickedAt, response.single().kickedAt)
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
    fun `신청중 목록은 승인 대기는 순번 없이 대기열 신청은 현재 순번과 함께 반환한다`() {
        val room = room(user(1L))
        val applicant = user(2L)
        val pending =
            ChatRoomJoinApplication(
                id = 20L,
                chatRoom = room,
                user = applicant,
                applicationMessage = "승인 대기",
                status = JoinApplicationStatus.PENDING,
            )
        val firstWaiter =
            ChatRoomJoinApplication(
                id = 21L,
                chatRoom = room,
                user = user(3L),
                applicationMessage = "첫 대기",
                status = JoinApplicationStatus.WAITLISTED,
            )
        val myWaitlist =
            ChatRoomJoinApplication(
                id = 22L,
                chatRoom = room,
                user = applicant,
                applicationMessage = "내 대기",
                status = JoinApplicationStatus.WAITLISTED,
            )
        `when`(
            applicationRepository.findAllByUserIdAndStatusInOrderByCreatedDateTimeDesc(
                2L,
                listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED),
            ),
        ).thenReturn(listOf(pending, myWaitlist))
        `when`(
            applicationRepository.findAllByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(
                10L,
                JoinApplicationStatus.WAITLISTED,
            ),
        ).thenReturn(listOf(firstWaiter, myWaitlist))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(3L)

        val response = service.getMyWaitingRooms(2L)

        assertEquals(listOf(null, 2), response.map { it.waitlistPosition })
        assertEquals(listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED), response.map { it.applicationStatus })
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
    fun `참가자가 아닌 사용자가 나가면 승인 대기 또는 대기열 신청을 취소한다`() {
        val room = room(user(1L))
        val pending =
            ChatRoomJoinApplication(
                id = 20L,
                chatRoom = room,
                user = user(2L),
                applicationMessage = "승인 대기",
                status = JoinApplicationStatus.PENDING,
            )
        val waitlisted =
            ChatRoomJoinApplication(
                id = 21L,
                chatRoom = room,
                user = user(3L),
                applicationMessage = "대기열",
                status = JoinApplicationStatus.WAITLISTED,
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        val activeStatuses = listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED)
        `when`(
            applicationRepository.findFirstByChatRoomIdAndUserIdAndStatusInOrderByCreatedDateTimeDescIdDesc(
                10L,
                2L,
                activeStatuses,
            ),
        ).thenReturn(pending)
        `when`(
            applicationRepository.findFirstByChatRoomIdAndUserIdAndStatusInOrderByCreatedDateTimeDescIdDesc(
                10L,
                3L,
                activeStatuses,
            ),
        ).thenReturn(waitlisted)

        val pendingResponse = service.leaveRoom(2L, 10L)
        val waitlistResponse = service.leaveRoom(3L, 10L)

        assertEquals("APPLICATION_CANCELLED", pendingResponse.result.name)
        assertEquals("WAITLIST_CANCELLED", waitlistResponse.result.name)
        verify(applicationRepository).delete(pending)
        verify(applicationRepository).delete(waitlisted)
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
        creator.information?.profileFileName = "user/profile/image/creator.webp"
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
        `when`(objectStorageRepository.getDownloadUrl("user/profile/image/creator.webp"))
            .thenReturn("https://cdn.example.com/creator.webp")

        val response = service.getCourse(5L)

        assertEquals("울릉도 대표 코스", response.title)
        assertEquals("바다와 산을 함께 즐기는 코스", response.description)
        assertEquals("여행자1", response.creatorNickname)
        assertEquals("https://cdn.example.com/creator.webp", response.creatorProfileImageUrl)
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
    fun `작성자 비공개 코스는 작성자와 원본 여행 및 평점 정보 없이 반환한다`() {
        val course =
            TravelCourse(
                id = 5L,
                type = TravelCourseType.CUSTOM,
                owner = user(1L),
                title = "작성자 비공개 코스",
            ).also { it.publish(showCreatorNickname = false) }
        `when`(courseRepository.findByIdAndType(5L, TravelCourseType.PUBLIC)).thenReturn(course)
        `when`(roomRepository.countByCourseIdAndStatusNot(5L, ChatRoomStatus.CANCELLED)).thenReturn(0L)
        `when`(ratingRepository.findAverageByCourseId(5L)).thenReturn(null)
        `when`(ratingRepository.countByCourseId(5L)).thenReturn(0L)

        val response = service.getCourse(5L)

        assertEquals(null, response.creatorNickname)
        assertEquals(null, response.creatorProfileImageUrl)
        assertEquals(null, response.creatorTravelStartDate)
        assertEquals(null, response.creatorTravelEndDate)
        assertEquals(null, response.averageRating)
        assertEquals(null, response.thumbnail)
        assertEquals(emptyList<Any>(), response.places)
    }

    @Test
    fun `탈퇴 작성자의 공개 코스는 저장된 작성자 닉네임을 반환한다`() {
        val course =
            TravelCourse(
                id = 5L,
                type = TravelCourseType.PUBLIC,
                title = "보존된 공개 코스",
                creatorNickname = "탈퇴한 여행자",
            )
        `when`(courseRepository.findByIdAndType(5L, TravelCourseType.PUBLIC)).thenReturn(course)
        `when`(roomRepository.countByCourseIdAndStatusNot(5L, ChatRoomStatus.CANCELLED)).thenReturn(0L)
        `when`(ratingRepository.findAverageByCourseId(5L)).thenReturn(null)
        `when`(ratingRepository.countByCourseId(5L)).thenReturn(0L)

        val response = service.getCourse(5L)

        assertEquals("탈퇴한 여행자", response.creatorNickname)
        assertEquals(null, response.creatorTravelStartDate)
        assertEquals(null, response.creatorTravelEndDate)
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
    fun `공개 코스 검색은 검색어 앞뒤 공백을 제거한다`() {
        `when`(courseRepository.searchPublicCourses("경주 야경")).thenReturn(emptyList())

        val response = service.searchPublicCourses("  경주 야경  ")

        assertTrue(response.isEmpty())
        verify(courseRepository).searchPublicCourses("경주 야경")
    }

    @Test
    fun `공개 코스 검색은 null 또는 공백 검색어를 전체 조회 조건으로 전달한다`() {
        `when`(courseRepository.searchPublicCourses(null)).thenReturn(emptyList())

        assertTrue(service.searchPublicCourses(null).isEmpty())
        assertTrue(service.searchPublicCourses("   ").isEmpty())

        verify(courseRepository, org.mockito.Mockito.times(2)).searchPublicCourses(null)
    }

    @Test
    fun `인기 코스는 당일 시간 숙박 기간 정보 없음과 장소 간 거리를 계산해 반환한다`() {
        val contentType = TourismContentType(12, "관광지")
        val dayCourse =
            TravelCourse(id = 11L, type = TravelCourseType.CUSTOM, title = "당일 코스", durationMinutes = 90L).also { course ->
                course.addCustomPlace(
                    TourismContent(
                        contentId = 101L,
                        contentType = contentType,
                        title = "출발지",
                        latitude = 37.5665,
                        longitude = 126.9780,
                    ),
                    1,
                    1,
                    LocalTime.of(9, 0),
                )
                course.addCustomPlace(
                    TourismContent(
                        contentId = 102L,
                        contentType = contentType,
                        title = "도착지",
                        latitude = 37.5512,
                        longitude = 126.9882,
                    ),
                    1,
                    2,
                    LocalTime.of(11, 0),
                )
                course.publish()
            }
        val exactHour = TravelCourse(id = 12L, type = TravelCourseType.PUBLIC, title = "두 시간 코스", durationMinutes = 120L)
        val overnight = TravelCourse(id = 13L, type = TravelCourseType.PUBLIC, title = "숙박 코스", tripNights = 1, tripDays = 2)
        val unknown = TravelCourse(id = 14L, type = TravelCourseType.PUBLIC, title = "미정 코스")
        `when`(courseRepository.findPopularPublicCourses(PageRequest.of(0, 3)))
            .thenReturn(listOf(dayCourse, exactHour, overnight, unknown))

        val response = service.getPopularPublicCourses()

        assertEquals(listOf("1시간 30분", "2시간", "1박 2일", "정보 없음"), response.map { it.travelTime })
        assertEquals(true, response.first().distanceKm > 0.0)
        assertEquals(2, response.first().places.size)
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
    fun `내 채팅방 찜 목록은 찜 저장소의 최신순 결과만 반환한다`() {
        val firstStartDate = LocalDate.of(2026, 9, 2)
        val secondStartDate = LocalDate.of(2026, 9, 1)
        val first = room(user(1L), startDate = firstStartDate, recruitmentDeadlineDate = firstStartDate.minusDays(1))
        val second = room(user(1L), startDate = secondStartDate, recruitmentDeadlineDate = secondStartDate.minusDays(1))
        `when`(favoriteRepository.findChatRoomsByUserIdOrderByFavoritedAtDesc(2L)).thenReturn(listOf(first, second))

        val response = service.getFavoriteRooms(2L)

        assertEquals(listOf(first.id, second.id), response.map { it.roomId })
        assertEquals(listOf(true, true), response.map { it.favorite })
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
    fun `완료한 여행의 첫 코스 평가는 새 평점으로 저장한다`() {
        val reviewer = user(2L)
        val room = room(user(1L))
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.hasCompletedTrip(10L, 2L, LocalDate.now())).thenReturn(true)
        `when`(ratingRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(null)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(reviewer))
        `when`(ratingRepository.save(any(TravelCourseRating::class.java))).thenAnswer { it.arguments[0] }

        service.rateCourse(2L, 10L, 4)

        val captor = ArgumentCaptor.forClass(TravelCourseRating::class.java)
        verify(ratingRepository).save(captor.capture())
        assertEquals(4, captor.value.score)
        assertEquals(10L, captor.value.chatRoom.id)
    }

    @Test
    fun `완료하지 않은 여행의 코스는 평가할 수 없다`() {
        val room = room(user(1L))
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(participantRepository.hasCompletedTrip(10L, 2L, LocalDate.now())).thenReturn(false)

        val exception = assertThrows(BaseException::class.java) { service.rateCourse(2L, 10L, 4) }

        assertEquals(ErrorCode.TRAVEL_COURSE_RATING_NOT_ALLOWED, exception.errorCode)
        verifyNoInteractions(ratingRepository, userRepository)
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

        val exception = assertThrows(BaseException::class.java) { service.createRoom(1L, request, nonEmptyThumbnail()) }

        assertEquals(ErrorCode.INVALID_TRIP_SCHEDULE, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `최소 출발 인원은 3명 이상이고 최대 참가 인원을 넘을 수 없다`() {
        val tooSmall = createRoomRequest(TripType.DAY_TRIP, endDate = null).copy(minimumParticipants = 2)
        val tooLarge = createRoomRequest(TripType.DAY_TRIP, endDate = null).copy(minimumParticipants = 5)

        assertEquals(
            ErrorCode.INVALID_MINIMUM_PARTICIPANTS,
            assertThrows(BaseException::class.java) { service.createRoom(1L, tooSmall, nonEmptyThumbnail()) }.errorCode,
        )
        assertEquals(
            ErrorCode.INVALID_MINIMUM_PARTICIPANTS,
            assertThrows(BaseException::class.java) { service.createRoom(1L, tooLarge, nonEmptyThumbnail()) }.errorCode,
        )
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `최대 참가 인원은 호스트를 포함해 20명까지 설정할 수 있다`() {
        val room = room(user(1L), maxParticipants = 20)

        assertEquals(20, room.maxParticipants)
    }

    @Test
    fun `과거 여행 시작일 과거 모집 마감일과 시작일 이후의 모집 마감일은 채팅방을 만들 수 없다`() {
        val pastStartDate =
            createRoomRequest(TripType.DAY_TRIP, endDate = null).copy(
                startDate = LocalDate.now().minusDays(1),
                recruitmentDeadlineDate = LocalDate.now(),
            )
        val pastRecruitmentDeadlineDate =
            createRoomRequest(TripType.DAY_TRIP, endDate = null).copy(
                startDate = LocalDate.now().plusDays(1),
                recruitmentDeadlineDate = LocalDate.now().minusDays(1),
            )
        val invalidDeadline =
            createRoomRequest(TripType.DAY_TRIP, endDate = null).copy(
                startDate = LocalDate.now().plusDays(3),
                recruitmentDeadlineDate = LocalDate.now().plusDays(4),
            )

        assertEquals(
            ErrorCode.PAST_CHAT_ROOM_START_DATE,
            assertThrows(BaseException::class.java) { service.createRoom(1L, pastStartDate, nonEmptyThumbnail()) }.errorCode,
        )
        assertEquals(
            ErrorCode.PAST_RECRUITMENT_DEADLINE_DATE,
            assertThrows(BaseException::class.java) { service.createRoom(1L, pastRecruitmentDeadlineDate, nonEmptyThumbnail()) }.errorCode,
        )
        assertEquals(
            ErrorCode.INVALID_RECRUITMENT_DEADLINE,
            assertThrows(BaseException::class.java) { service.createRoom(1L, invalidDeadline, nonEmptyThumbnail()) }.errorCode,
        )
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

        val exception = assertThrows(BaseException::class.java) { service.createRoom(1L, request, nonEmptyThumbnail()) }

        assertEquals(ErrorCode.INVALID_TRIP_SCHEDULE, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `빈 채팅방 썸네일은 생성할 수 없다`() {
        val thumbnail = mock(MultipartFile::class.java)
        `when`(thumbnail.isEmpty).thenReturn(true)

        val exception =
            assertThrows(BaseException::class.java) {
                service.createRoom(1L, createRoomRequest(TripType.DAY_TRIP, endDate = null), thumbnail)
            }

        assertEquals(ErrorCode.CHAT_ROOM_THUMBNAIL_REQUIRED, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `이미지가 아닌 채팅방 썸네일은 생성할 수 없다`() {
        val thumbnail = mock(MultipartFile::class.java)
        `when`(thumbnail.isEmpty).thenReturn(false)
        `when`(thumbnail.size).thenReturn(10L)
        `when`(thumbnail.contentType).thenReturn("text/plain")

        val exception =
            assertThrows(BaseException::class.java) {
                service.createRoom(1L, createRoomRequest(TripType.DAY_TRIP, endDate = null), thumbnail)
            }

        assertEquals(ErrorCode.INVALID_CHAT_ROOM_THUMBNAIL, exception.errorCode)
        verifyNoInteractions(userRepository, fhdWebpImageOptimizer, objectStorageRepository)
    }

    @Test
    fun `커스텀 코스로 채팅방을 만들면 코스 장소 태그 호스트와 개설 메시지를 저장한다`() {
        val host = profiledUser(1L, Gender.F, LocalDate.now().minusYears(30))
        val contentType = TourismContentType(12, "관광지")
        val first = TourismContent(contentId = 100L, contentType = contentType, title = "서울역")
        val second = TourismContent(contentId = 101L, contentType = contentType, title = "남산")
        val tag = TravelCourseTag(id = 7L, name = "힐링")
        val thumbnail = mock(MultipartFile::class.java)
        val thumbnailBytes = byteArrayOf(1, 2, 3)
        val optimizedThumbnailBytes = byteArrayOf(4, 5, 6)
        val startDate = LocalDate.now().plusDays(10)
        val request =
            CreateChatRoomRequest(
                title = " 서울 당일 여행 ",
                description = " 함께 여행해요 ",
                minimumParticipants = 3,
                maxParticipants = 4,
                tripType = TripType.DAY_TRIP,
                startDate = startDate,
                recruitmentDeadlineDate = startDate.minusDays(3),
                dayTripStartTime = LocalTime.of(9, 0),
                dayTripEndTime = LocalTime.of(18, 0),
                meetingDateTime = startDate.atTime(8, 30),
                genderRestriction = GenderRestriction.NONE,
                joinApprovalMode = JoinApprovalMode.MANUAL,
                courseType = TravelCourseType.CUSTOM,
                customCourse =
                    CreateCustomCourseRequest(
                        title = " 서울 명소 코스 ",
                        description = " 대표 명소 ",
                        places = listOf(place(100L, 1, 1, 9, 0), place(101L, 1, 2, 11, 0)),
                        tagIds = setOf(7L),
                    ),
            )
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(host))
        `when`(courseRepository.saveAndFlush(any(TravelCourse::class.java))).thenAnswer { it.arguments[0] }
        `when`(tourismContentRepository.findByContentId(100L)).thenReturn(first)
        `when`(tourismContentRepository.findByContentId(101L)).thenReturn(second)
        `when`(tagRepository.findAllById(setOf(7L))).thenReturn(listOf(tag))
        `when`(thumbnail.isEmpty).thenReturn(false)
        `when`(thumbnail.size).thenReturn(thumbnailBytes.size.toLong())
        `when`(thumbnail.contentType).thenReturn("image/png")
        `when`(thumbnail.bytes).thenReturn(thumbnailBytes)
        `when`(
            fhdWebpImageOptimizer.optimizeToFhdWebp(thumbnailBytes, ErrorCode.INVALID_CHAT_ROOM_THUMBNAIL),
        ).thenReturn(optimizedThumbnailBytes)
        `when`(
            objectStorageRepository.upload(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                anyByteArray(),
                org.mockito.ArgumentMatchers.anyString(),
            ),
        ).thenReturn("chat-room/thumbnail/cover.webp")
        `when`(objectStorageRepository.getDownloadUrl("chat-room/thumbnail/cover.webp"))
            .thenReturn("https://cdn.example.com/cover.webp")
        `when`(roomRepository.saveAndFlush(any(ChatRoom::class.java))).thenAnswer { it.arguments[0] }
        `when`(participantRepository.saveAndFlush(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        service.createRoom(1L, request, thumbnail)

        val roomCaptor = ArgumentCaptor.forClass(ChatRoom::class.java)
        verify(roomRepository).saveAndFlush(roomCaptor.capture())
        assertEquals("서울 당일 여행", roomCaptor.value.roomTitle)
        assertEquals("함께 여행해요", roomCaptor.value.description)
        assertEquals(3, roomCaptor.value.minimumParticipants)
        assertEquals(2, roomCaptor.value.course.places.size)
        assertEquals(540L, roomCaptor.value.course.durationMinutes)
        assertEquals("https://cdn.example.com/cover.webp", roomCaptor.value.thumbnail)
        verify(fhdWebpImageOptimizer).optimizeToFhdWebp(thumbnailBytes, ErrorCode.INVALID_CHAT_ROOM_THUMBNAIL)
        val uploadInvocation =
            org.mockito.Mockito
                .mockingDetails(objectStorageRepository)
                .invocations
                .single { it.method.name == "upload" && it.arguments.size == 4 }
        assertEquals("chat-room/thumbnail/", uploadInvocation.arguments[0])
        assertTrue((uploadInvocation.arguments[1] as String).endsWith(".webp"))
        assertEquals(optimizedThumbnailBytes.toList(), (uploadInvocation.arguments[2] as ByteArray).toList())
        assertEquals("image/webp", uploadInvocation.arguments[3])
        verify(placeRepository, org.mockito.Mockito.times(2)).save(any())
        verify(notificationService).notifyRoomCreated(roomCaptor.value)
    }

    @Test
    fun `등록된 공개 코스로 채팅방을 만들면 기존 코스를 그대로 연결한다`() {
        val host = profiledUser(1L, Gender.M, LocalDate.now().minusYears(30))
        val publicCourse = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "공개 코스")
        val thumbnail = mock(MultipartFile::class.java)
        val thumbnailBytes = byteArrayOf(1, 2, 3)
        val optimizedThumbnailBytes = byteArrayOf(4, 5, 6)
        val startDate = LocalDate.now().plusDays(10)
        val request =
            CreateChatRoomRequest(
                title = "공개 코스 여행",
                minimumParticipants = 3,
                maxParticipants = 3,
                tripType = TripType.OVERNIGHT,
                startDate = startDate,
                endDate = startDate.plusDays(1),
                recruitmentDeadlineDate = startDate.minusDays(3),
                meetingDateTime = startDate.atTime(8, 0),
                genderRestriction = GenderRestriction.NONE,
                joinApprovalMode = JoinApprovalMode.AUTO,
                courseType = TravelCourseType.PUBLIC,
                courseId = 5L,
            )
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(host))
        `when`(courseRepository.findByIdAndType(5L, TravelCourseType.PUBLIC)).thenReturn(publicCourse)
        `when`(thumbnail.isEmpty).thenReturn(false)
        `when`(thumbnail.size).thenReturn(thumbnailBytes.size.toLong())
        `when`(thumbnail.contentType).thenReturn("image/jpeg")
        `when`(thumbnail.bytes).thenReturn(thumbnailBytes)
        `when`(
            fhdWebpImageOptimizer.optimizeToFhdWebp(thumbnailBytes, ErrorCode.INVALID_CHAT_ROOM_THUMBNAIL),
        ).thenReturn(optimizedThumbnailBytes)
        `when`(
            objectStorageRepository.upload(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                anyByteArray(),
                org.mockito.ArgumentMatchers.anyString(),
            ),
        ).thenReturn("chat-room/thumbnail/cover.webp")
        `when`(objectStorageRepository.getDownloadUrl("chat-room/thumbnail/cover.webp"))
            .thenReturn("https://cdn.example.com/cover.webp")
        `when`(roomRepository.saveAndFlush(any(ChatRoom::class.java))).thenAnswer { it.arguments[0] }
        `when`(participantRepository.saveAndFlush(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        service.createRoom(1L, request, thumbnail)

        val roomCaptor = ArgumentCaptor.forClass(ChatRoom::class.java)
        verify(roomRepository).saveAndFlush(roomCaptor.capture())
        assertEquals(publicCourse, roomCaptor.value.course)
        assertEquals("https://cdn.example.com/cover.webp", roomCaptor.value.thumbnail)
        verifyNoInteractions(placeRepository, tagRepository, tourismContentRepository)
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
    fun `집합 위치는 위도와 경도를 함께 입력해야 한다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        val request =
            UpdateMeetingInfoRequest(
                meetingLatitude = 37.5547,
                meetingLongitude = null,
                meetingDateTime = room.startDate.atTime(8, 0),
            )

        val exception = assertThrows(BaseException::class.java) { service.updateMeetingInfo(1L, 10L, request) }

        assertEquals(ErrorCode.INVALID_MEETING_INFORMATION, exception.errorCode)
        verifyNoInteractions(messageRepository)
    }

    @Test
    fun `집합 일시는 여행 시작일 이후로 정할 수 없다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        val request = UpdateMeetingInfoRequest(meetingDateTime = room.startDate.plusDays(1).atStartOfDay())

        val exception = assertThrows(BaseException::class.java) { service.updateMeetingInfo(1L, 10L, request) }

        assertEquals(ErrorCode.INVALID_MEETING_INFORMATION, exception.errorCode)
        verifyNoInteractions(messageRepository)
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
    fun `호스트가 신청자를 승인할 때 자리가 있으면 참가자로 전환하고 신청을 삭제한다`() {
        val host = user(1L)
        val applicant = profiledUser(3L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(host)
        val application =
            ChatRoomJoinApplication(id = 30L, chatRoom = room, user = applicant, applicationMessage = "함께 가고 싶어요")
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(applicationRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(application)
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(2L)
        `when`(participantRepository.save(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        val response = service.approveApplication(1L, 10L, 30L)

        assertEquals("JOINED", response.result.name)
        assertEquals(null, response.waitlistPosition)
        verify(applicationRepository).delete(application)
        verify(participantRepository).save(any(ChatRoomParticipant::class.java))
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
    fun `여행 시작 하루 전부터는 참가 신청을 받지 않는다`() {
        val startDate = LocalDate.now().plusDays(1)
        val room =
            room(
                host = user(1L),
                startDate = startDate,
                endDate = startDate.plusDays(1),
                recruitmentDeadlineDate = LocalDate.now(),
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)

        val exception =
            assertThrows(BaseException::class.java) {
                service.applyToJoin(2L, 10L, JoinChatRoomRequest("신청합니다"))
            }

        assertEquals(ErrorCode.CHAT_ROOM_CLOSED, exception.errorCode)
        verifyNoInteractions(userRepository, applicationRepository, messageRepository)
    }

    @Test
    fun `수동 승인 신청은 호스트 승인 대기 결과를 반환한다`() {
        val host = user(1L)
        val applicant = user(2L)
        val room = room(host)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))
        `when`(applicationRepository.saveAndFlush(any(ChatRoomJoinApplication::class.java)))
            .thenReturn(
                ChatRoomJoinApplication(
                    id = 30L,
                    chatRoom = room,
                    user = applicant,
                    applicationMessage = "함께 가고 싶어요",
                ),
            )

        val response = service.applyToJoin(2L, 10L, JoinChatRoomRequest("함께 가고 싶어요"))

        assertEquals("PENDING_APPROVAL", response.result.name)
        assertEquals(30L, response.applicationId)
        assertEquals(JoinApplicationStatus.PENDING, response.applicationStatus)
        val savedApplication = ArgumentCaptor.forClass(ChatRoomJoinApplication::class.java)
        verify(applicationRepository).saveAndFlush(savedApplication.capture())
        assertEquals(JoinApplicationStatus.PENDING, savedApplication.value.status)
    }

    @Test
    fun `자동 승인 방이 가득 차면 대기열 결과를 반환한다`() {
        val host = user(1L)
        val applicant = user(2L)
        val room = room(host, joinApprovalMode = JoinApprovalMode.AUTO)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(3L)
        `when`(applicationRepository.saveAndFlush(any(ChatRoomJoinApplication::class.java)))
            .thenReturn(
                ChatRoomJoinApplication(
                    id = 31L,
                    chatRoom = room,
                    user = applicant,
                    applicationMessage = "함께 가고 싶어요",
                    status = JoinApplicationStatus.WAITLISTED,
                ),
            )

        val response = service.applyToJoin(2L, 10L, JoinChatRoomRequest("함께 가고 싶어요"))

        assertEquals("WAITLISTED", response.result.name)
        assertEquals(31L, response.applicationId)
        assertEquals(JoinApplicationStatus.WAITLISTED, response.applicationStatus)
        val savedApplication = ArgumentCaptor.forClass(ChatRoomJoinApplication::class.java)
        verify(applicationRepository).saveAndFlush(savedApplication.capture())
        assertEquals(JoinApplicationStatus.WAITLISTED, savedApplication.value.status)
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
    fun `채팅방 상세는 성별 제한을 충족하지 않으면 canApply를 false로 반환한다`() {
        val applicant = profiledUser(2L, Gender.M, LocalDate.now().minusYears(30))
        val room = room(user(1L), genderRestriction = GenderRestriction.FEMALE_ONLY)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val response = service.getRoom(2L, 10L)

        assertEquals(false, response.canApply)
    }

    @Test
    fun `채팅방 상세는 연령 조건을 충족하면 canApply를 true로 반환한다`() {
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L), minimumAge = 25, maximumAge = 35)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val response = service.getRoom(2L, 10L)

        assertEquals(true, response.canApply)
    }

    @Test
    fun `채팅방 상세는 연령 제한을 충족하지 않으면 canApply를 false로 반환한다`() {
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L), maximumAge = 25)
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))

        val response = service.getRoom(2L, 10L)

        assertEquals(false, response.canApply)
    }

    @Test
    fun `마감된 방이거나 이미 참가 신청 중이면 채팅방 상세의 canApply는 false다`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val closedRoom =
            room(
                host = user(1L),
                startDate = tomorrow,
                endDate = tomorrow.plusDays(1),
                recruitmentDeadlineDate = LocalDate.now(),
            )
        val openRoom = room(user(1L))
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(closedRoom), Optional.of(openRoom))
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)))
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 2L)).thenReturn(true)

        val closedResponse = service.getRoom(2L, 10L)
        val duplicateResponse = service.getRoom(2L, 10L)

        assertEquals(false, closedResponse.canApply)
        assertEquals(false, duplicateResponse.canApply)
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
    fun `수동 승인 방 참가 신청은 호스트에게 전할 말을 정리해 승인 대기로 저장한다`() {
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L), joinApprovalMode = JoinApprovalMode.MANUAL)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))
        `when`(applicationRepository.saveAndFlush(any(ChatRoomJoinApplication::class.java))).thenAnswer { it.arguments[0] }

        val response = service.applyToJoin(2L, 10L, JoinChatRoomRequest("  사진 찍는 것을 좋아해요  "))

        val captor = ArgumentCaptor.forClass(ChatRoomJoinApplication::class.java)
        verify(applicationRepository).saveAndFlush(captor.capture())
        assertEquals(JoinApplicationStatus.PENDING, captor.value.status)
        assertEquals("사진 찍는 것을 좋아해요", captor.value.applicationMessage)
        assertEquals("PENDING_APPROVAL", response.result.name)
        assertEquals(captor.value.id, response.applicationId)
        assertEquals(JoinApplicationStatus.PENDING, response.applicationStatus)
    }

    @Test
    fun `자동 승인 방이 정원이면 조건을 충족한 신청자를 대기열에 저장한다`() {
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(user(1L), joinApprovalMode = JoinApprovalMode.AUTO)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(applicant))
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(3L)
        `when`(applicationRepository.saveAndFlush(any(ChatRoomJoinApplication::class.java))).thenAnswer { it.arguments[0] }

        val response = service.applyToJoin(2L, 10L, JoinChatRoomRequest("대기할게요"))

        val captor = ArgumentCaptor.forClass(ChatRoomJoinApplication::class.java)
        verify(applicationRepository).saveAndFlush(captor.capture())
        assertEquals(JoinApplicationStatus.WAITLISTED, captor.value.status)
        assertEquals("WAITLISTED", response.result.name)
        assertEquals(captor.value.id, response.applicationId)
        assertEquals(JoinApplicationStatus.WAITLISTED, response.applicationStatus)
        verify(participantRepository, org.mockito.Mockito.never()).saveAndFlush(any(ChatRoomParticipant::class.java))
    }

    @Test
    fun `이미 참가했거나 활성 신청이 있으면 중복 참가 신청을 거부한다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 2L)).thenReturn(true)

        val exception =
            assertThrows(BaseException::class.java) {
                service.applyToJoin(2L, 10L, JoinChatRoomRequest("신청합니다"))
            }

        assertEquals(ErrorCode.CHAT_ROOM_ALREADY_JOINED, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `호스트는 승인 대기 신청자의 프로필과 신청 메시지를 조회한다`() {
        val host = user(1L)
        val applicant = profiledUser(2L, Gender.F, LocalDate.now().minusYears(28))
        val room = room(host)
        val application = mock(ChatRoomJoinApplication::class.java)
        `when`(application.id).thenReturn(30L)
        `when`(application.user).thenReturn(applicant)
        `when`(application.applicationMessage).thenReturn("사진 찍는 것을 좋아해요")
        `when`(application.createdDateTime).thenReturn(LocalDateTime.of(2026, 8, 23, 10, 0))
        `when`(roomRepository.findById(10L)).thenReturn(Optional.of(room))
        `when`(
            applicationRepository.findAllByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(
                10L,
                JoinApplicationStatus.PENDING,
            ),
        ).thenReturn(listOf(application))
        `when`(participantRepository.countCompletedTrips(2L)).thenReturn(4L)

        val response = service.getPendingApplications(1L, 10L)

        assertEquals(1, response.size)
        assertEquals("사진 찍는 것을 좋아해요", response.single().applicationMessage)
        assertEquals(4, response.single().applicant.completedTripCount)
    }

    @Test
    fun `호스트가 승인 대기 신청을 거절하면 거절 상태가 유지된다`() {
        val host = user(1L)
        val room = room(host)
        val application =
            ChatRoomJoinApplication(
                id = 30L,
                chatRoom = room,
                user = user(2L),
                applicationMessage = "신청합니다",
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(applicationRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(application)

        service.rejectApplication(1L, 10L, 30L)

        assertEquals(JoinApplicationStatus.REJECTED, application.status)
    }

    @Test
    fun `호스트는 모집 중인 여행을 확정할 수 있다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        service.changeStatus(1L, 10L, ChatRoomStatus.CONFIRMED)

        assertEquals(ChatRoomStatus.CONFIRMED, room.status)
        val captor = ArgumentCaptor.forClass(ChatMessage::class.java)
        verify(messageRepository).saveAndFlush(captor.capture())
        assertEquals("여행이 확정되었어요.", captor.value.content)
    }

    @Test
    fun `호스트는 모집 중인 여행을 취소할 수 있다`() {
        val room = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        service.changeStatus(1L, 10L, ChatRoomStatus.CANCELLED)

        assertEquals(ChatRoomStatus.CANCELLED, room.status)
        val captor = ArgumentCaptor.forClass(ChatMessage::class.java)
        verify(messageRepository).saveAndFlush(captor.capture())
        assertEquals("여행이 불발되었어요.", captor.value.content)
    }

    @Test
    fun `모집 중이 아니거나 모집 중 상태로의 변경은 거부한다`() {
        val confirmedRoom = room(user(1L), status = ChatRoomStatus.CONFIRMED)
        val recruitingRoom = room(user(1L))
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(confirmedRoom, recruitingRoom)

        val closedRoomException =
            assertThrows(BaseException::class.java) {
                service.changeStatus(1L, 10L, ChatRoomStatus.CANCELLED)
            }
        val sameStatusException =
            assertThrows(BaseException::class.java) {
                service.changeStatus(1L, 10L, ChatRoomStatus.RECRUITING)
            }

        assertEquals(ErrorCode.INVALID_CHAT_ROOM_STATUS, closedRoomException.errorCode)
        assertEquals(ErrorCode.INVALID_CHAT_ROOM_STATUS, sameStatusException.errorCode)
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

    @Test
    fun `호스트가 방을 나가면 모임을 취소하고 호스트 참가 관계를 삭제한다`() {
        val host = profiledUser(1L, Gender.F, LocalDate.now().minusYears(30))
        val room = room(host)
        val participant = ChatRoomParticipant(chatRoom = room, user = host, role = ChatParticipantRole.HOST)
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 1L)).thenReturn(participant)
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        val response = service.leaveRoom(1L, 10L)

        assertEquals(ChatRoomStatus.CANCELLED, room.status)
        assertEquals("HOST_LEFT_AND_ROOM_CANCELLED", response.result.name)
        verify(participantRepository).delete(participant)
        verify(participantRepository).flush()
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

    private fun message(
        id: Long,
        room: ChatRoom,
        sender: User,
        type: ChatMessageType = ChatMessageType.USER,
        content: String = "메시지 $id",
    ): ChatMessage =
        mock(ChatMessage::class.java).also {
            `when`(it.id).thenReturn(id)
            `when`(it.chatRoom).thenReturn(room)
            `when`(it.sender).thenReturn(sender)
            `when`(it.type).thenReturn(type)
            `when`(it.content).thenReturn(content)
            `when`(it.createdDateTime).thenReturn(LocalDateTime.of(2026, 8, 22, 18, 0).plusMinutes(id))
            `when`(it.mentionedUsers).thenReturn(emptySet())
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

    private fun nonEmptyThumbnail(): MultipartFile = mock(MultipartFile::class.java)

    private fun anyByteArray(): ByteArray = org.mockito.ArgumentMatchers.any(ByteArray::class.java) ?: ByteArray(0)

    private fun createRoomRequest(
        tripType: TripType,
        endDate: LocalDate?,
        dayTripStartTime: LocalTime? = LocalTime.of(9, 0),
        dayTripEndTime: LocalTime? = LocalTime.of(18, 0),
    ) = CreateChatRoomRequest(
        title = "테스트 여행",
        minimumParticipants = 3,
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

    private fun anyInputStream(): InputStream {
        any(InputStream::class.java)
        return ByteArrayInputStream(byteArrayOf())
    }

    private fun room(
        host: User,
        course: TravelCourse = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "울릉도 대표 코스"),
        genderRestriction: GenderRestriction = GenderRestriction.NONE,
        minimumAge: Int? = null,
        maximumAge: Int? = null,
        joinApprovalMode: JoinApprovalMode = JoinApprovalMode.MANUAL,
        startDate: LocalDate = LocalDate.now().plusDays(10),
        endDate: LocalDate? = LocalDate.now().plusDays(11),
        dayTripStartTime: LocalTime? = null,
        dayTripEndTime: LocalTime? = null,
        recruitmentDeadlineDate: LocalDate = LocalDate.now().plusDays(5),
        status: ChatRoomStatus = ChatRoomStatus.RECRUITING,
        thumbnail: String? = "https://cdn.example.com/chat-room.png",
        meetingDetails: String? = null,
        meetingLatitude: Double? = 36.0322,
        meetingLongitude: Double? = 129.3747,
        maxParticipants: Int = 3,
    ) = ChatRoom(
        id = 10L,
        host = host,
        course = course,
        roomTitle = "울릉도 여행",
        thumbnail = thumbnail,
        maxParticipants = maxParticipants,
        startDate = startDate,
        endDate = endDate,
        dayTripStartTime = dayTripStartTime,
        dayTripEndTime = dayTripEndTime,
        recruitmentDeadlineDate = recruitmentDeadlineDate,
        meetingLatitude = meetingLatitude,
        meetingLongitude = meetingLongitude,
        meetingDetails = meetingDetails,
        meetingDateTime = startDate.atStartOfDay(),
        participationFee = 100000L,
        genderRestriction = genderRestriction,
        minimumAge = minimumAge,
        maximumAge = maximumAge,
        joinApprovalMode = joinApprovalMode,
        status = status,
    )
}
