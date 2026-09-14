package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.entity.Entity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import jakarta.persistence.LockModeType
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCoursePlace
import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.time.LocalDate

interface ChatRoomRepository :
    JpaRepository<ChatRoom, Long>,
    ChatRoomCustomRepository {
    fun findAllByStatusAndRecruitmentDeadlineDateBetween(
        status: ChatRoomStatus,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ChatRoom>

    fun findFirstByCourseIdAndHostIdAndStatusOrderByStartDateAsc(
        courseId: Long,
        hostId: Long,
        status: ChatRoomStatus,
    ): ChatRoom?

    fun countByCourseIdAndStatusNot(
        courseId: Long,
        status: ChatRoomStatus,
    ): Long
}

interface ChatRoomCustomRepository {
    // BE-02: 목록과 지도가 같은 필터(tagId)를 받아 같은 결과 집합을 주도록 맞췄다.
    /**
     * 탐색 목록. [cursor] 는 **직전 페이지의 마지막 방 id** 이고, 그보다 작은 id 만 준다.
     * 정렬을 `id DESC` 하나로 둔 이유가 여기 있다 — 커서가 id 인데 다른 값으로 정렬하면
     * 경계에서 한 건을 건너뛰거나 두 번 보여 준다.
     */
    fun searchRooms(
        userId: Long,
        blockedUserIds: Collection<Long>,
        keyword: String?,
        tagId: Long?,
        today: LocalDate,
        cursor: Long?,
        pageable: Pageable,
    ): List<ChatRoom>

    fun findRecruitingRoomsByPublicCourseId(
        userId: Long,
        blockedUserIds: Collection<Long>,
        courseId: Long,
        today: LocalDate,
        pageable: Pageable,
    ): List<ChatRoom>

    fun findMapRooms(
        userId: Long,
        blockedUserIds: Collection<Long>,
        tagId: Long?,
        today: LocalDate,
        minimumLatitude: Double,
        maximumLatitude: Double,
        minimumLongitude: Double,
        maximumLongitude: Double,
        crossesDateLine: Boolean,
    ): List<ChatRoom>

    fun findAllStartingRoomsWithoutSystemEvent(
        status: ChatRoomStatus,
        date: LocalDate,
        eventKey: String,
    ): List<ChatRoom>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdForUpdate(id: Long): ChatRoom?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllExpiredRecruitingRoomsForUpdate(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllDeletionDueRoomsForUpdate(date: LocalDate): List<ChatRoom>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllCompletedRoomsWithoutDeletionScheduleForUpdate(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom>

    fun findAllCompletedConfirmedRooms(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom>

    /**
     * 채팅을 잠글 때가 된 방 — 여행이 [date] 이전에 끝났고 아직 안 잠긴 확정 방.
     *
     * **삭제 예약(`deletionScheduledDate`)과는 별개다.** 잠금은 「더 못 쓴다」이고,
     * 삭제는 「기록이 사라진다」라 시점이 다르다(잠금 +7일 · 삭제 +14일).
     * 그래서 `findAllCompletedRoomsWithoutDeletionScheduleForUpdate` 와 달리
     * 삭제 예약 여부를 보지 않는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllChatCloseDueRoomsForUpdate(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom>

    fun existsCompletedHostRoom(
        hostId: Long,
        courseId: Long,
        status: ChatRoomStatus,
        date: LocalDate,
    ): Boolean

    fun completeForTest(
        roomId: Long,
        startDate: LocalDate,
        recruitmentDeadlineDate: LocalDate,
        endDate: LocalDate?,
    ): Int
}

class ChatRoomCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : ChatRoomCustomRepository {
    override fun findMapRooms(
        userId: Long,
        blockedUserIds: Collection<Long>,
        tagId: Long?,
        today: LocalDate,
        minimumLatitude: Double,
        maximumLatitude: Double,
        minimumLongitude: Double,
        maximumLongitude: Double,
        crossesDateLine: Boolean,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)
                val course = entity(TravelCourse::class)
                val tag = entity(TravelCourseTag::class)
                val blockedParticipant = entity(ChatRoomParticipant::class)
                val myParticipant = entity(ChatRoomParticipant::class)
                val longitude = room.path(ChatRoom::meetingLongitude)

                // BE-02: 목록(searchRooms)과 같은 태그 조건을 태울 수 있도록 코스·태그를 조인한다.
                selectDistinct(room)
                    .from(
                        room,
                        innerJoin(room.path(ChatRoom::course)).`as`(course),
                        leftJoin(course.path(TravelCourse::courseTags)).`as`(tag),
                    ).whereAnd(
                        room.path(ChatRoom::status).eq(ChatRoomStatus.RECRUITING),
                        room.path(ChatRoom::recruitmentDeadlineDate).ge(today),
                        tagId?.let { tag.path(TravelCourseTag::id).eq(it) },
                        room.path(ChatRoom::meetingLatitude).ge(minimumLatitude),
                        room.path(ChatRoom::meetingLatitude).le(maximumLatitude),
                        if (crossesDateLine) {
                            or(longitude.ge(minimumLongitude), longitude.le(maximumLongitude))
                        } else {
                            and(longitude.ge(minimumLongitude), longitude.le(maximumLongitude))
                        },
                        room.path(ChatRoom::host).path(User::id).notIn(blockedUserIds),
                        notExists(
                            select(blockedParticipant.path(ChatRoomParticipant::id))
                                .from(blockedParticipant)
                                .whereAnd(
                                    blockedParticipant.path(ChatRoomParticipant::chatRoom).path(ChatRoom::id).eq(room.path(ChatRoom::id)),
                                    blockedParticipant.path(ChatRoomParticipant::user).path(User::id).`in`(blockedUserIds),
                                ).asSubquery(),
                        ),
                        notExists(
                            select(myParticipant.path(ChatRoomParticipant::id))
                                .from(myParticipant)
                                .whereAnd(
                                    myParticipant.path(ChatRoomParticipant::chatRoom).path(ChatRoom::id).eq(room.path(ChatRoom::id)),
                                    myParticipant.path(ChatRoomParticipant::user).path(User::id).eq(userId),
                                ).asSubquery(),
                        ),
                    )
            }.filterNotNull()

    override fun searchRooms(
        userId: Long,
        blockedUserIds: Collection<Long>,
        keyword: String?,
        tagId: Long?,
        today: LocalDate,
        cursor: Long?,
        pageable: Pageable,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll(pageable) {
                val room = entity(ChatRoom::class)
                val course = entity(TravelCourse::class)
                val tag = entity(TravelCourseTag::class)
                val coursePlace = entity(TravelCoursePlace::class)
                val tourismContent = entity(TourismContent::class)
                val blockedParticipant = entity(ChatRoomParticipant::class)
                val myParticipant = entity(ChatRoomParticipant::class)

                selectDistinct(room)
                    .from(
                        room,
                        innerJoin(room.path(ChatRoom::course)).`as`(course),
                        leftJoin(course.path(TravelCourse::courseTags)).`as`(tag),
                        leftJoin(coursePlace).on(coursePlace.path(TravelCoursePlace::course).eq(course)),
                        leftJoin(tourismContent).on(tourismContent.eq(coursePlace.path(TravelCoursePlace::tourismContent))),
                    ).whereAnd(
                        room.path(ChatRoom::status).eq(ChatRoomStatus.RECRUITING),
                        room.path(ChatRoom::recruitmentDeadlineDate).ge(today),
                        // 무한 스크롤 커서. 첫 페이지는 null 이라 지금까지와 똑같이 동작한다.
                        cursor?.let { room.path(ChatRoom::id).lt(it) },
                        // BE-02: 지도와 같은 태그 조건. 생략하면 지금까지와 똑같이 전체를 조회한다.
                        tagId?.let { tag.path(TravelCourseTag::id).eq(it) },
                        keyword?.let { keyword ->
                            val pattern = "%${keyword.lowercase()}%"
                            or(
                                lower(room.path(ChatRoom::roomTitle)).like(pattern),
                                lower(room.path(ChatRoom::description)).like(pattern),
                                lower(course.path(TravelCourse::title)).like(pattern),
                                lower(tag.path(TravelCourseTag::name)).like(pattern),
                                lower(tourismContent.path(TourismContent::title)).like(pattern),
                                lower(tourismContent.path(TourismContent::address1)).like(pattern),
                                lower(tourismContent.path(TourismContent::address2)).like(pattern),
                            )
                        },
                        room.path(ChatRoom::host).path(User::id).notIn(blockedUserIds),
                        notExists(
                            select(blockedParticipant.path(ChatRoomParticipant::id))
                                .from(blockedParticipant)
                                .whereAnd(
                                    blockedParticipant.path(ChatRoomParticipant::chatRoom).path(ChatRoom::id).eq(room.path(ChatRoom::id)),
                                    blockedParticipant.path(ChatRoomParticipant::user).path(User::id).`in`(blockedUserIds),
                                ).asSubquery(),
                        ),
                        notExists(
                            select(myParticipant.path(ChatRoomParticipant::id))
                                .from(myParticipant)
                                .whereAnd(
                                    myParticipant.path(ChatRoomParticipant::chatRoom).path(ChatRoom::id).eq(room.path(ChatRoom::id)),
                                    myParticipant.path(ChatRoomParticipant::user).path(User::id).eq(userId),
                                ).asSubquery(),
                        ),
                    ).orderBy(room.path(ChatRoom::id).desc())
            }.filterNotNull()

    override fun findRecruitingRoomsByPublicCourseId(
        userId: Long,
        blockedUserIds: Collection<Long>,
        courseId: Long,
        today: LocalDate,
        pageable: Pageable,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll(pageable) {
                val room = entity(ChatRoom::class)
                val blockedParticipant = entity(ChatRoomParticipant::class)
                val myParticipant = entity(ChatRoomParticipant::class)

                select(room)
                    .from(room)
                    .whereAnd(
                        room.path(ChatRoom::course).path(TravelCourse::id).eq(courseId),
                        room.path(ChatRoom::status).eq(ChatRoomStatus.RECRUITING),
                        room.path(ChatRoom::recruitmentDeadlineDate).ge(today),
                        room.path(ChatRoom::host).path(User::id).notIn(blockedUserIds),
                        notExists(
                            select(blockedParticipant.path(ChatRoomParticipant::id))
                                .from(blockedParticipant)
                                .whereAnd(
                                    blockedParticipant.path(ChatRoomParticipant::chatRoom).path(ChatRoom::id).eq(room.path(ChatRoom::id)),
                                    blockedParticipant.path(ChatRoomParticipant::user).path(User::id).`in`(blockedUserIds),
                                ).asSubquery(),
                        ),
                        notExists(
                            select(myParticipant.path(ChatRoomParticipant::id))
                                .from(myParticipant)
                                .whereAnd(
                                    myParticipant.path(ChatRoomParticipant::chatRoom).path(ChatRoom::id).eq(room.path(ChatRoom::id)),
                                    myParticipant.path(ChatRoomParticipant::user).path(User::id).eq(userId),
                                ).asSubquery(),
                        ),
                    ).orderBy(
                        room.path(ChatRoom::createdDateTime).desc(),
                        room.path(ChatRoom::id).desc(),
                    )
            }.filterNotNull()

    override fun findAllStartingRoomsWithoutSystemEvent(
        status: ChatRoomStatus,
        date: LocalDate,
        eventKey: String,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)
                val message = entity(ChatMessage::class)

                select(room)
                    .from(room)
                    .whereAnd(
                        room.path(ChatRoom::status).eq(status),
                        room.path(ChatRoom::startDate).eq(date),
                        notExists(
                            select(message.path(ChatMessage::id))
                                .from(message)
                                .whereAnd(
                                    message.path(ChatMessage::chatRoom).path(ChatRoom::id).eq(room.path(ChatRoom::id)),
                                    message.path(ChatMessage::systemEventKey).eq(eventKey),
                                ).asSubquery(),
                        ),
                    )
            }.filterNotNull()

    override fun findByIdForUpdate(id: Long): ChatRoom? =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)

                select(room)
                    .from(room)
                    .where(room.path(ChatRoom::id).eq(id))
            }.firstOrNull()

    override fun findAllExpiredRecruitingRoomsForUpdate(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)

                select(room)
                    .from(room)
                    .whereAnd(
                        room.path(ChatRoom::status).eq(status),
                        room.path(ChatRoom::recruitmentDeadlineDate).lt(date),
                    )
            }.filterNotNull()

    override fun findAllDeletionDueRoomsForUpdate(date: LocalDate): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)

                select(room)
                    .from(room)
                    .where(room.path(ChatRoom::deletionScheduledDate).le(date))
            }.filterNotNull()

    override fun findAllCompletedRoomsWithoutDeletionScheduleForUpdate(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)

                select(room)
                    .from(room)
                    .whereAnd(
                        room.path(ChatRoom::status).eq(status),
                        room.path(ChatRoom::deletionScheduledDate).isNull(),
                        room.path(ChatRoom::chatClosedDateTime).isNull(),
                        completedTripPredicate(room, date),
                    )
            }.filterNotNull()

    override fun findAllChatCloseDueRoomsForUpdate(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)

                select(room)
                    .from(room)
                    .whereAnd(
                        room.path(ChatRoom::status).eq(status),
                        // 이미 잠긴 방은 건드리지 않는다 — 잠금 시각을 뒤로 미루면 안 된다.
                        room.path(ChatRoom::chatClosedDateTime).isNull(),
                        completedTripPredicate(room, date),
                    )
            }.filterNotNull()

    override fun findAllCompletedConfirmedRooms(
        status: ChatRoomStatus,
        date: LocalDate,
    ): List<ChatRoom> =
        kotlinJdslJpqlExecutor
            .findAll {
                val room = entity(ChatRoom::class)

                select(room)
                    .from(room)
                    .whereAnd(
                        room.path(ChatRoom::status).eq(status),
                        completedTripPredicate(room, date),
                    )
            }.filterNotNull()

    override fun existsCompletedHostRoom(
        hostId: Long,
        courseId: Long,
        status: ChatRoomStatus,
        date: LocalDate,
    ): Boolean =
        kotlinJdslJpqlExecutor
            .findAll(limit = 1) {
                val existsRoot = entity(User::class, "existsRoot")
                val room = entity(ChatRoom::class, "room")

                select(
                    caseWhen(
                        exists(
                            select(room.path(ChatRoom::id))
                                .from(room)
                                .whereAnd(
                                    room.path(ChatRoom::host).path(User::id).eq(hostId),
                                    room.path(ChatRoom::course).path(TravelCourse::id).eq(courseId),
                                    room.path(ChatRoom::status).eq(status),
                                    completedTripPredicate(room, date),
                                ).asSubquery(),
                        ),
                    ).then(true).`else`(false),
                ).from(existsRoot)
            }.firstOrNull() ?: false

    override fun completeForTest(
        roomId: Long,
        startDate: LocalDate,
        recruitmentDeadlineDate: LocalDate,
        endDate: LocalDate?,
    ): Int {
        if (endDate == null) {
            return kotlinJdslJpqlExecutor.update {
                val room = entity(ChatRoom::class)

                update(room)
                    .set(room.path(ChatRoom::status), ChatRoomStatus.CONFIRMED)
                    .set(room.path(ChatRoom::startDate), startDate)
                    .set(room.path(ChatRoom::recruitmentDeadlineDate), recruitmentDeadlineDate)
                    .set(room.path(ChatRoom::chatClosedDateTime), null)
                    .set(room.path(ChatRoom::deletionScheduledDate), null)
                    .where(room.path(ChatRoom::id).eq(roomId))
            }
        }

        return kotlinJdslJpqlExecutor.update {
            val room = entity(ChatRoom::class)

            update(room)
                .set(room.path(ChatRoom::status), ChatRoomStatus.CONFIRMED)
                .set(room.path(ChatRoom::startDate), startDate)
                .set(room.path(ChatRoom::endDate), endDate)
                .set(room.path(ChatRoom::recruitmentDeadlineDate), recruitmentDeadlineDate)
                .set(room.path(ChatRoom::chatClosedDateTime), null)
                .set(room.path(ChatRoom::deletionScheduledDate), null)
                .where(room.path(ChatRoom::id).eq(roomId))
        }
    }

    private fun Jpql.completedTripPredicate(
        room: Entity<ChatRoom>,
        date: LocalDate,
    ) = or(
        and(
            room.path(ChatRoom::endDate).isNull(),
            room.path(ChatRoom::startDate).lt(date),
        ),
        room.path(ChatRoom::endDate).lt(date),
    )
}
