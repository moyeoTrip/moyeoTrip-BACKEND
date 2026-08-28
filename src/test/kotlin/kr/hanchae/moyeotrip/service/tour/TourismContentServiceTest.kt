package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourCommonDetailItem
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentImage
import kr.hanchae.moyeotrip.entity.tour.TourismContentImageType
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
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
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val service =
        TourismContentService(
            tourApiClient,
            repository,
            contentTypeRepository,
            imageRepository,
            objectStorageRepository,
        )

    @Test
    fun `여행코스를 제외한 관광 타입 목록을 반환한다`() {
        `when`(contentTypeRepository.findAllByCodeNotOrderByCodeAsc(25))
            .thenReturn(listOf(TourismContentType(12, "관광지"), TourismContentType(39, "음식점")))

        val response = service.getContentTypes()

        assertEquals(listOf(12, 39), response.map { it.contentTypeId })
    }

    @Test
    fun `여행코스 타입은 관광지 목록으로 조회할 수 없다`() {
        val exception =
            assertThrows(BaseException::class.java) {
                service.getContents(contentTypeId = 25, keyword = null, page = 1, size = 20)
            }

        assertEquals(ErrorCode.TOURISM_COURSE_CONTENT_NOT_LISTED, exception.errorCode)
        verifyNoInteractions(repository)
    }

    @Test
    fun `관광 타입을 지정하면 해당 타입의 페이지를 반환한다`() {
        val pageable = PageRequest.of(1, 2, Sort.by("title").ascending())
        val content = content(100L, 12, "주산지")
        `when`(repository.searchListableContents(25, 12, null, pageable))
            .thenReturn(PageImpl(listOf(content), pageable, 5))

        val response = service.getContents(contentTypeId = 12, keyword = null, page = 2, size = 2)

        assertEquals(2, response.page)
        assertEquals(5, response.totalElements)
        assertEquals("주산지", response.items.single().title)
        verify(repository).searchListableContents(25, 12, null, pageable)
    }

    @Test
    fun `관광 타입을 생략하면 여행코스를 제외한 페이지를 반환한다`() {
        val pageable = PageRequest.of(0, 20, Sort.by("title").ascending())
        `when`(repository.searchListableContents(25, null, null, pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val response = service.getContents(contentTypeId = null, keyword = null, page = 1, size = 20)

        assertEquals(1, response.page)
        assertEquals(0, response.totalElements)
        verify(repository).searchListableContents(25, null, null, pageable)
    }

    @Test
    fun `여행지 검색어와 관광 타입을 함께 적용해 제목과 주소 검색 패턴으로 전달한다`() {
        val pageable = PageRequest.of(0, 20, Sort.by("title").ascending())
        val content = content(100L, 12, "토박이 식당")
        `when`(repository.searchListableContents(25, 12, "%토박이%", pageable))
            .thenReturn(PageImpl(listOf(content), pageable, 1))

        val response = service.getContents(contentTypeId = 12, keyword = "  토박이  ", page = 1, size = 20)

        assertEquals("토박이 식당", response.items.single().title)
        verify(repository).searchListableContents(25, 12, "%토박이%", pageable)
    }

    @Test
    fun `빈 검색어는 전체 조회 조건으로 전달한다`() {
        val pageable = PageRequest.of(0, 20, Sort.by("title").ascending())
        `when`(repository.searchListableContents(25, null, null, pageable)).thenReturn(PageImpl(emptyList()))

        service.getContents(contentTypeId = null, keyword = "  ", page = 1, size = 20)

        verify(repository).searchListableContents(25, null, null, pageable)
    }

    @Test
    fun `없는 관광 콘텐츠 상세는 조회할 수 없다`() {
        `when`(repository.findByContentId(999L)).thenReturn(null)

        val exception = assertThrows(BaseException::class.java) { service.getContent(999L) }

        assertEquals(ErrorCode.TOURISM_CONTENT_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `상세 조회는 저장된 이미지가 없어도 외부 이미지 API를 호출하지 않는다`() {
        val content = content(100L, 12, "주산지")
        `when`(repository.findByContentId(100L)).thenReturn(content)
        `when`(tourApiClient.getCommonDetail(100L))
            .thenReturn(TourCommonDetailItem(contentid = "100", telname = " 안내 ", homepage = " ", overview = " 소개 "))
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.CONTENT))
            .thenReturn(emptyList())

        val response = service.getContent(100L)

        assertEquals("안내", response.telephoneName)
        assertEquals(null, response.homepage)
        assertEquals("소개", response.overview)
        assertEquals(emptyList<Any>(), response.contentImages)
        assertEquals(emptyList<Any>(), response.menuImages)
        verify(tourApiClient, never()).getImages(100L, "Y")
        verify(tourApiClient, never()).getImages(100L, "N")
        verify(imageRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList())
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

    @Test
    fun `Object Storage에 저장된 관광 이미지는 CDN URL을 우선 반환한다`() {
        val content =
            content(200L, 39, "맛집", telephoneName = "예약 문의").apply {
                updateThumbnail("tourism/image/thumbnail.jpg")
            }
        val contentImage = image(content, TourismContentImageType.CONTENT, "tourism/image/content.jpg")
        `when`(repository.findByContentId(200L)).thenReturn(content)
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.CONTENT))
            .thenReturn(listOf(contentImage))
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.MENU))
            .thenReturn(emptyList())
        `when`(objectStorageRepository.getDownloadUrl("tourism/image/thumbnail.jpg")).thenReturn("https://cdn/thumbnail.jpg")
        `when`(objectStorageRepository.getDownloadUrl("tourism/image/content.jpg")).thenReturn("https://cdn/content.jpg")

        val response = service.getContent(200L)

        assertEquals("https://cdn/thumbnail.jpg", response.thumbnail)
        assertEquals("https://cdn/content.jpg", response.contentImages.single().originalImageUrl)
    }

    @Test
    fun `기존 HTTP 관광 이미지 URL은 원본 URL을 유지하다 Object Storage 저장 성공 시에만 교체한다`() {
        val content =
            content(200L, 12, "주산지", telephoneName = "관광 안내").apply {
                updateThumbnail("http://tong.visitkorea.or.kr/thumbnail.jpg")
            }
        val contentImage =
            image(content, TourismContentImageType.CONTENT, "http://tong.visitkorea.or.kr/content.jpg")
        `when`(repository.findByContentId(200L)).thenReturn(content)
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.CONTENT))
            .thenReturn(listOf(contentImage))

        val response = service.getContent(200L)

        assertEquals("http://tong.visitkorea.or.kr/thumbnail.jpg", response.thumbnail)
        assertEquals("http://tong.visitkorea.or.kr/content.jpg", response.contentImages.single().originalImageUrl)
        assertEquals("http://tong.visitkorea.or.kr/content.jpg", contentImage.originalImageUrl)
    }

    @Test
    fun `기존 상세 정보가 있어도 전화 안내명이 비어 있으면 공공데이터 상세를 다시 보완한다`() {
        val content = content(300L, 12, "주왕산", telephoneName = null).apply { updateCommonDetail(null, "기존 홈페이지", "기존 소개") }
        `when`(repository.findByContentId(300L)).thenReturn(content)
        `when`(tourApiClient.getCommonDetail(300L))
            .thenReturn(TourCommonDetailItem(contentid = "300", telname = "관광안내소"))
        `when`(imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(1L, TourismContentImageType.CONTENT))
            .thenReturn(listOf(image(content, TourismContentImageType.CONTENT, "https://content")))

        val response = service.getContent(300L)

        assertEquals("관광안내소", response.telephoneName)
        assertEquals("기존 홈페이지", response.homepage)
        assertEquals("기존 소개", response.overview)
        verify(tourApiClient).getCommonDetail(300L)
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
