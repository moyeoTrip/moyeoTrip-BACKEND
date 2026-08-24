package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentRepository :
    JpaRepository<TourismContent, Long>,
    TourismContentCustomRepository {
    fun findByContentId(contentId: Long): TourismContent?

    fun findAllByContentTypeCode(
        contentTypeCode: Int,
        pageable: Pageable,
    ): Page<TourismContent>

    fun findAllByContentTypeCodeNot(
        contentTypeCode: Int,
        pageable: Pageable,
    ): Page<TourismContent>

    fun findAllByContentTypeCode(contentTypeCode: Int): List<TourismContent>

    fun findAllByContentIdIn(contentIds: Collection<Long>): List<TourismContent>
}

interface TourismContentCustomRepository {
    fun searchListableContents(
        courseContentTypeId: Int,
        contentTypeId: Int?,
        keywordPattern: String?,
        pageable: Pageable,
    ): Page<TourismContent>
}

class TourismContentCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : TourismContentCustomRepository {
    override fun searchListableContents(
        courseContentTypeId: Int,
        contentTypeId: Int?,
        keywordPattern: String?,
        pageable: Pageable,
    ): Page<TourismContent> =
        kotlinJdslJpqlExecutor.findPage(pageable) {
            val content = entity(TourismContent::class)
            val contentTypeCode = content.path(TourismContent::contentType).path(TourismContentType::code)

            select(content)
                .from(content)
                .whereAnd(
                    contentTypeCode.ne(courseContentTypeId),
                    contentTypeId?.let { contentTypeCode.eq(it) },
                    keywordPattern?.let { pattern ->
                        or(
                            lower(content.path(TourismContent::title)).like(pattern),
                            lower(content.path(TourismContent::address1)).like(pattern),
                            lower(content.path(TourismContent::address2)).like(pattern),
                        )
                    },
                )
        }
}
