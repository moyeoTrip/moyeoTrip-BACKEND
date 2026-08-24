package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class TourismContentRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var tourismContentRepository: TourismContentRepository

    @Autowired
    private lateinit var tourismContentTypeRepository: TourismContentTypeRepository

    @Nested
    inner class SearchListableContents {
        @Test
        fun `코스 타입을 제외하고 제목 또는 주소 검색어에 일치하는 여행지만 조회한다`() {
            val attractionType = tourismContentTypeRepository.saveAndFlush(TourismContentType(12, "관광지"))
            val restaurantType = tourismContentTypeRepository.saveAndFlush(TourismContentType(39, "음식점"))
            val courseType = tourismContentTypeRepository.saveAndFlush(TourismContentType(25, "여행코스"))
            val titleMatched = savedContent(1001L, attractionType, title = "주왕산 국립공원")
            val addressMatched = savedContent(1002L, restaurantType, title = "산골 식당", address1 = "청송 주왕산로 1")
            savedContent(1003L, attractionType, title = "경주 동궁")
            savedContent(1004L, courseType, title = "주왕산 추천 코스")

            val result = tourismContentRepository.searchListableContents(25, null, "%주왕산%", PageRequest.of(0, 10))

            assertEquals(setOf(titleMatched.id, addressMatched.id), result.content.map(TourismContent::id).toSet())
        }

        @Test
        fun `요청한 관광 타입으로 결과를 제한한다`() {
            val attractionType = tourismContentTypeRepository.saveAndFlush(TourismContentType(12, "관광지"))
            val restaurantType = tourismContentTypeRepository.saveAndFlush(TourismContentType(39, "음식점"))
            val attraction = savedContent(2001L, attractionType, title = "주왕산 국립공원")
            savedContent(2002L, restaurantType, title = "주왕산 식당")

            val result = tourismContentRepository.searchListableContents(25, 12, "%주왕산%", PageRequest.of(0, 10))

            assertEquals(listOf(attraction.id), result.content.map(TourismContent::id))
        }
    }

    private fun savedContent(
        contentId: Long,
        contentType: TourismContentType,
        title: String,
        address1: String? = null,
    ): TourismContent =
        tourismContentRepository.saveAndFlush(
            TourismContent(
                contentId = contentId,
                contentType = contentType,
                title = title,
                address1 = address1,
            ),
        )
}
