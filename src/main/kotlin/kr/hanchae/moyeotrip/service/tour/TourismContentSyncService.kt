package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourAreaBasedItem
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TourismContentTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class TourismContentSyncService(
    private val tourApiClient: TourApiClient,
    private val repository: TourismContentRepository,
    private val contentTypeRepository: TourismContentTypeRepository,
) {
    @Transactional
    fun syncGyeongsangbukdo(): Int {
        val contentTypes = contentTypeRepository.findAll().associateBy { it.code }
        val apiItems = contentTypes.keys.flatMap(::fetchAllItems)
        val existingByContentId = repository.findAll().associateBy(TourismContent::contentId)
        val entities =
            apiItems.distinctBy(TourAreaBasedItem::contentid).map { item ->
                val contentType =
                    contentTypes[item.contenttypeid.toInt()]
                        ?: throw BaseException(ErrorCode.TOURISM_CONTENT_TYPE_NOT_FOUND)
                existingByContentId[item.contentid.toLong()]?.apply { updateFrom(item, contentType) }
                    ?: item.toEntity(contentType)
            }
        repository.saveAll(entities)
        return entities.size
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

    private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)

    private fun String.toTourApiDateTimeOrNull(): LocalDateTime? =
        takeIf { it.length == 14 }
            ?.let { runCatching { LocalDateTime.parse(it, TOUR_API_DATE_TIME_FORMATTER) }.getOrNull() }

    companion object {
        private const val PAGE_SIZE = 1000
        private val TOUR_API_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}
