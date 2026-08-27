package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourAreaBasedItem
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TourismContentTypeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class TourismContentSyncService(
    private val tourApiClient: TourApiClient,
    private val repository: TourismContentRepository,
    private val contentTypeRepository: TourismContentTypeRepository,
    private val tourismImageProxyService: TourismImageProxyService,
    private val objectStorageRepository: ObjectStorageRepository,
) {
    fun syncGyeongsangbukdo(): Int {
        val contentTypes = contentTypeRepository.findAll().associateBy { it.code }
        val apiItems = contentTypes.keys.flatMap(::fetchAllItems)
        val existingByContentId = repository.findAll().associateBy(TourismContent::contentId)
        val uniqueItems = apiItems.distinctBy(TourAreaBasedItem::contentid)
        uniqueItems.chunked(SAVE_BATCH_SIZE).forEachIndexed { index, items ->
            val entities =
                items.map { item ->
                    val contentType =
                        contentTypes[item.contenttypeid.toInt()]
                            ?: throw BaseException(ErrorCode.TOURISM_CONTENT_TYPE_NOT_FOUND)
                    val entity = existingByContentId[item.contentid.toLong()] ?: item.toEntity(contentType)
                    val previousThumbnail = entity.thumbnail
                    val previousSourceModifiedDateTime = entity.sourceModifiedDateTime
                    entity.updateFrom(item, contentType)
                    entity.storeThumbnailIfNeeded(previousThumbnail, previousSourceModifiedDateTime)
                    entity
                }
            repository.saveAllAndFlush(entities)
            val completedCount = ((index + 1) * SAVE_BATCH_SIZE).coerceAtMost(uniqueItems.size)
            logger.info("관광 콘텐츠와 썸네일 이관 진행: {}/{}건", completedCount, uniqueItems.size)
        }
        return uniqueItems.size
    }

    private fun fetchAllItems(contentTypeId: Int): List<TourAreaBasedItem> {
        val firstPage = tourApiClient.getAreaBasedTourismInformation(contentTypeId, pageNo = 1, numOfRows = PAGE_SIZE)
        if (firstPage.totalCount <= firstPage.items.size) return firstPage.items

        val pageCount = (firstPage.totalCount + PAGE_SIZE - 1) / PAGE_SIZE
        return buildList {
            addAll(firstPage.items)
            for (pageNo in 2..pageCount) {
                addAll(tourApiClient.getAreaBasedTourismInformation(contentTypeId, pageNo, PAGE_SIZE).items)
            }
        }
    }

    private fun TourAreaBasedItem.toEntity(contentType: TourismContentType) =
        TourismContent(
            contentId = contentid.toLong(),
            contentType = contentType,
            title = title,
        ).apply { updateFrom(this@toEntity, contentType) }

    private fun TourismContent.updateFrom(
        item: TourAreaBasedItem,
        contentType: TourismContentType,
    ) {
        update(
            contentType = contentType,
            title = item.title,
            address1 = item.addr1.nullIfBlank(),
            address2 = item.addr2.nullIfBlank(),
            zipcode = item.zipcode.nullIfBlank(),
            telephone = item.tel.nullIfBlank(),
            thumbnail = item.firstimage.nullIfBlank(),
            copyrightType = item.cpyrhtDivCd.nullIfBlank(),
            longitude = item.mapx.toDoubleOrNull(),
            latitude = item.mapy.toDoubleOrNull(),
            mapLevel = item.mlevel.nullIfBlank(),
            sourceCreatedDateTime = item.createdtime.toTourApiDateTimeOrNull(),
            sourceModifiedDateTime = item.modifiedtime.toTourApiDateTimeOrNull(),
            regionCode = item.lDongRegnCd.nullIfBlank(),
            signguCode = item.lDongSignguCd.nullIfBlank(),
            level1Code = item.lclsSystm1.nullIfBlank(),
            level2Code = item.lclsSystm2.nullIfBlank(),
            level3Code = item.lclsSystm3.nullIfBlank(),
        )
    }

    private fun TourismContent.storeThumbnailIfNeeded(
        previousThumbnail: String?,
        previousSourceModifiedDateTime: LocalDateTime?,
    ) {
        val sourceUrl = thumbnail ?: return
        if (previousThumbnail?.startsWith(ObjectStorageRepository.TOURISM_IMAGE_PATH) == true &&
            previousSourceModifiedDateTime == sourceModifiedDateTime
        ) {
            updateThumbnail(previousThumbnail)
            return
        }
        runCatching {
            val image = tourismImageProxyService.getImage(sourceUrl)
            objectStorageRepository.uploadTourismImage(image.bytes, image.contentType.toString())
        }.onSuccess(::updateThumbnail)
            .onFailure { exception ->
                logger.warn("관광 콘텐츠 {} 썸네일의 Object Storage 이관에 실패했습니다. sourceUrl={}", contentId, sourceUrl, exception)
            }
    }

    private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)

    private fun String.toTourApiDateTimeOrNull(): LocalDateTime? =
        takeIf { it.length == 14 }
            ?.let { runCatching { LocalDateTime.parse(it, TOUR_API_DATE_TIME_FORMATTER) }.getOrNull() }

    companion object {
        private val logger = LoggerFactory.getLogger(TourismContentSyncService::class.java)
        private const val PAGE_SIZE = 1000
        private const val SAVE_BATCH_SIZE = 50
        private val TOUR_API_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}
