package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentImage
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentRepository :
    JpaRepository<TourismContent, Long>,
    TourismContentCustomRepository {
    fun findByContentId(contentId: Long): TourismContent?
}

interface TourismContentCustomRepository {
    fun searchListableContents(
        courseContentTypeId: Int,
        contentTypeId: Int?,
        keywordPattern: String?,
        pageable: Pageable,
    ): Page<TourismContent>

    fun findContentIdsWithoutImagesAfter(
        lastContentId: Long?,
        limit: Int,
    ): List<Long>
}

class TourismContentCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : TourismContentCustomRepository {
    override fun findContentIdsWithoutImagesAfter(
        lastContentId: Long?,
        limit: Int,
    ): List<Long> =
        kotlinJdslJpqlExecutor
            .findAll(limit = limit) {
                val content = entity(TourismContent::class)
                val image = entity(TourismContentImage::class)
                val contentId = content.path(TourismContent::contentId)
                val contentPrimaryKey = content.path(TourismContent::id)
                val imageContentPrimaryKey = image.path(TourismContentImage::tourismContent).path(TourismContent::id)

                select(contentId)
                    .from(content)
                    .whereAnd(
                        notExists(
                            select(image.path(TourismContentImage::id))
                                .from(image)
                                .whereAnd(imageContentPrimaryKey.eq(contentPrimaryKey))
                                .asSubquery(),
                        ),
                        lastContentId?.let { contentId.gt(it) },
                    ).orderBy(contentId.asc())
            }.filterNotNull()

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
