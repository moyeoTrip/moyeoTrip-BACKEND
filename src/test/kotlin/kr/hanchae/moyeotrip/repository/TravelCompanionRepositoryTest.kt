package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.TravelCompanion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TravelCompanionRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var travelCompanionRepository: TravelCompanionRepository

    @Nested
    inner class AverageMannerScoreByCompanionId {
        @Test
        fun `평가된 매너 점수만 평균으로 반환한다`() {
            val owner = savedUser()
            val companion = savedUser()
            val reviewer = savedUser()
            val course = savedCourse()
            val room = savedRoom(owner, course)
            val anotherRoom = savedRoom(owner, course, title = "다른 여행")
            travelCompanionRepository.saveAndFlush(
                TravelCompanion(
                    owner = owner,
                    companion = companion,
                    chatRoom = room,
                    mannerScore = 5,
                ),
            )
            travelCompanionRepository.saveAndFlush(
                TravelCompanion(
                    owner = reviewer,
                    companion = companion,
                    chatRoom = anotherRoom,
                ),
            )
            travelCompanionRepository.saveAndFlush(
                TravelCompanion(
                    owner = reviewer,
                    companion = companion,
                    chatRoom = room,
                    mannerScore = 3,
                ),
            )
            travelCompanionRepository.saveAndFlush(
                TravelCompanion(
                    owner = owner,
                    companion = reviewer,
                    chatRoom = room,
                ),
            )

            assertEquals(4.0, travelCompanionRepository.averageMannerScoreByCompanionId(companion.id))
            assertNull(travelCompanionRepository.averageMannerScoreByCompanionId(owner.id))
        }
    }

    @Nested
    inner class FindAllReviewedByCompanionId {
        @Test
        fun `대상 사용자에게 남겨진 한줄평만 최신순으로 조회한다`() {
            val firstReviewer = savedUser()
            val secondReviewer = savedUser()
            val target = savedUser()
            val otherUser = savedUser()
            val course = savedCourse()
            val room = savedRoom(firstReviewer, course)
            val firstReview =
                travelCompanionRepository.saveAndFlush(
                    TravelCompanion(
                        owner = firstReviewer,
                        companion = target,
                        chatRoom = room,
                        oneLineReview = "함께해서 즐거웠어요",
                    ),
                )
            val secondReview =
                travelCompanionRepository.saveAndFlush(
                    TravelCompanion(
                        owner = secondReviewer,
                        companion = target,
                        chatRoom = room,
                        oneLineReview = "약속을 잘 지켜요",
                    ),
                )
            travelCompanionRepository.saveAndFlush(
                TravelCompanion(
                    owner = firstReviewer,
                    companion = target,
                    chatRoom = savedRoom(firstReviewer, course, title = "한줄평 없음"),
                ),
            )
            travelCompanionRepository.saveAndFlush(
                TravelCompanion(
                    owner = firstReviewer,
                    companion = otherUser,
                    chatRoom = room,
                    oneLineReview = "다른 사용자 한줄평",
                ),
            )

            val reviews = travelCompanionRepository.findAllReviewedByCompanionId(target.id)

            assertEquals(listOf(secondReview.id, firstReview.id), reviews.map { it.id })
        }
    }
}
