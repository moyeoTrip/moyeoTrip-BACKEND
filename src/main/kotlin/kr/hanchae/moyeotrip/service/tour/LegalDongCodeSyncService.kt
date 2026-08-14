package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourLegalDongItem
import kr.hanchae.moyeotrip.entity.tour.LegalDongCode
import kr.hanchae.moyeotrip.repository.LegalDongCodeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LegalDongCodeSyncService(
    private val tourApiClient: TourApiClient,
    private val legalDongCodeRepository: LegalDongCodeRepository,
) {
    @Transactional
    fun syncGyeongsangbukdo(): Int {
        val apiItems = fetchAllItems()
        val existingByCode =
            legalDongCodeRepository
                .findAllByRegionCode(GYEONGSANGBUKDO_REGION_CODE)
                .associateBy(LegalDongCode::signguCode)

        val entities =
            apiItems.map { item ->
                existingByCode[item.lDongSignguCd]?.apply {
                    updateNames(item.lDongRegnNm, item.lDongSignguNm)
                } ?: item.toEntity()
            }
        legalDongCodeRepository.saveAll(entities)
        return entities.size
    }

    private fun fetchAllItems(): List<TourLegalDongItem> {
        val firstPage = tourApiClient.getGyeongsangbukdoLegalDongCodes(pageNo = 1, numOfRows = PAGE_SIZE)
        if (firstPage.totalCount <= firstPage.items.size) return firstPage.items

        val pageCount = (firstPage.totalCount + PAGE_SIZE - 1) / PAGE_SIZE
        return buildList {
            addAll(firstPage.items)
            for (pageNo in 2..pageCount) {
                addAll(tourApiClient.getGyeongsangbukdoLegalDongCodes(pageNo, PAGE_SIZE).items)
            }
        }
    }

    private fun TourLegalDongItem.toEntity() =
        LegalDongCode(
            regionCode = lDongRegnCd,
            signguCode = lDongSignguCd,
            regionName = lDongRegnNm,
            signguName = lDongSignguNm,
        )

    companion object {
        private const val GYEONGSANGBUKDO_REGION_CODE = "47"
        private const val PAGE_SIZE = 1000
    }
}
