package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourClassificationSystemItem
import kr.hanchae.moyeotrip.entity.tour.TourClassificationCode
import kr.hanchae.moyeotrip.repository.TourClassificationCodeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TourClassificationCodeSyncService(
    private val tourApiClient: TourApiClient,
    private val repository: TourClassificationCodeRepository,
) {
    @Transactional
    fun sync(): Int {
        val apiItems = fetchAllItems()
        val existingByLevel3Code = repository.findAll().associateBy(TourClassificationCode::level3Code)
        val entities =
            apiItems.map { item ->
                existingByLevel3Code[item.lclsSystm3Cd]?.apply {
                    updateNames(item.lclsSystm1Nm, item.lclsSystm2Nm, item.lclsSystm3Nm)
                } ?: item.toEntity()
            }
        repository.saveAll(entities)
        return entities.size
    }

    private fun fetchAllItems(): List<TourClassificationSystemItem> {
        val firstPage = tourApiClient.getClassificationSystemCodes(pageNo = 1, numOfRows = PAGE_SIZE)
        if (firstPage.totalCount <= firstPage.items.size) return firstPage.items

        val pageCount = (firstPage.totalCount + PAGE_SIZE - 1) / PAGE_SIZE
        return buildList {
            addAll(firstPage.items)
            for (pageNo in 2..pageCount) {
                addAll(tourApiClient.getClassificationSystemCodes(pageNo, PAGE_SIZE).items)
            }
        }
    }

    private fun TourClassificationSystemItem.toEntity() =
        TourClassificationCode(
            level1Code = lclsSystm1Cd,
            level2Code = lclsSystm2Cd,
            level3Code = lclsSystm3Cd,
            level1Name = lclsSystm1Nm,
            level2Name = lclsSystm2Nm,
            level3Name = lclsSystm3Nm,
        )

    companion object {
        private const val PAGE_SIZE = 1000
    }
}
