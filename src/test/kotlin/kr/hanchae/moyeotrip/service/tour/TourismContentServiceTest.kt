package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourCommonDetailItem
import kr.hanchae.moyeotrip.client.TourImageItem
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentImage
import kr.hanchae.moyeotrip.entity.tour.TourismContentImageType
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.TourismContentImageRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TourismContentTypeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class TourismContentServiceTest {
    private val tourApiClient = mock(TourApiClient::class.java)
    private val repository = mock(TourismContentRepository::class.java)
    private val contentTypeRepository = mock(TourismContentTypeRepository::class.java)
    private val imageRepository = mock(TourismContentImageRepository::class.java)
    private val service = TourismContentService(tourApiClient, repository, contentTypeRepository, imageRepository)

    @Test
    fun `여행코스를 제외한 관광 타입 목록을 반환한다`() {
        `when`(contentTypeRepository.findAllByCodeNotOrderByCodeAsc(25))
            .thenReturn(listOf(TourismContentType(12, "관광지"), TourismContentType(39, "음식점")))

        val response = service.getContentTypes()

        assertEquals(listOf(12, 39), response.map { it.contentTypeId })
    }

    @Test
    fun `여행코스 타입은 관광지 목록으로 조회할 수 없다`() {
        val exception = assertThrows(BaseException::class.java) { service.getContents(25, 0, 20) }

        assertEquals(ErrorCode.TOURISM_COURSE_CONTENT_NOT_LISTED, exception.errorCode)
        verifyNoInteractions(repository)
    }

    @Test
    fun `관광 타입을 지정하면 해당 타입의 페이지를 반환한다`() {
        val pageable = PageRequest.of(1, 2, Sort.by("title").ascending())
        val content = content(100L, 12, "주산지")
        `when`(repository.findAllByContentTypeCode(12, pageable))
            .thenReturn(PageImpl(listOf(content), pageable, 5))

        val response = service.getContents(12, 1, 2)

        assertEquals(1, response.page)
        assertEquals(5, response.totalElements)
        assertEquals("주산지", response.items.single().title)
    }

    @Test
    fun `관광 타입을 생략하면 여행코스를 제외한 페이지를 반환한다`() {
        val pageable = PageRequest.of(0, 20, Sort.by("title").ascending())
        `when`(repository.findAllByContentTypeCodeNot(25, pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val response = service.getContents(null, 0, 20)

        assertEquals(0, response.totalElements)
        verify(repository).findAllByContentTypeCodeNot(25, pageable)
    }

    @Test
    fun `없는 관광 콘텐츠 상세는 조회할 수 없다`() {
        `when`(repository.findByContentId(999L)).thenReturn(null)

        val exception = assertThrows(BaseException::class.java) { service.getContent(999L) }

        assertEquals(ErrorCode.TOURISM_CONTENT_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `일반 관광지 상세가 비어 있으면 공통 상세와 콘텐츠 이미지를 한 번 수집한다`() {
        val content = content(100L, 12, "주산지")
        val savedImage = image(content, TourismContentImageType.CONTENT, "https://image")
        `when`(repository.findByContentId(100L)).thenReturn(content)
        `when`(tourApiClient.getCommonDetail(100L))
            .thenReturn(TourCommonDetailItem(contentid = "100", telname = " 안내 ", homepage = " ", overview = " 소개 "))
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.CONTENT))
            .thenReturn(emptyList(), listOf(savedImage))
        `when`(tourApiClient.getImages(100L, "Y"))
            .thenReturn(listOf(TourImageItem(contentid = "100", imgname = " 전경 ", originimgurl = "https://image")))

        val response = service.getContent(100L)

        assertEquals("안내", response.telephoneName)
        assertEquals(null, response.homepage)
        assertEquals("소개", response.overview)
        assertEquals("https://image", response.contentImages.single().originalImageUrl)
        assertEquals(emptyList<Any>(), response.menuImages)
        verify(tourApiClient, never()).getImages(100L, "N")
        verify(imageRepository).saveAll(org.mockito.ArgumentMatchers.anyList())
    }

    @Test
    fun `음식점에 저장된 이미지가 있으면 외부 이미지를 다시 조회하지 않는다`() {
        val content = content(200L, 39, "맛집", telephoneName = "예약 문의")
        val contentImage = image(content, TourismContentImageType.CONTENT, "https://content")
        val menuImage = image(content, TourismContentImageType.MENU, "https://menu")
        `when`(repository.findByContentId(200L)).thenReturn(content)
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.CONTENT))
            .thenReturn(listOf(contentImage))
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.MENU))
            .thenReturn(listOf(menuImage))

        val response = service.getContent(200L)

        assertEquals("https://content", response.contentImages.single().originalImageUrl)
        assertEquals("https://menu", response.menuImages.single().originalImageUrl)
        verifyNoInteractions(tourApiClient)
    }

    private fun content(
        contentId: Long,
        contentTypeId: Int,
        title: String,
        telephoneName: String? = null,
    ) = TourismContent(
        id = 1L,
        contentId = contentId,
        contentType = TourismContentType(contentTypeId, "타입"),
        title = title,
        telephoneName = telephoneName,
    )

    private fun image(
        content: TourismContent,
        type: TourismContentImageType,
        url: String,
    ) = TourismContentImage(
        tourismContent = content,
        type = type,
        imageName = "이미지",
        originalImageUrl = url,
        serialNumber = "1",
        copyrightType = "Type1",
    )
}
