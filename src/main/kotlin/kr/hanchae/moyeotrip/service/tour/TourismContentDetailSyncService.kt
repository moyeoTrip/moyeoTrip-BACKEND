package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TourismContentDetailSyncService(
    private val tourApiClient: TourApiClient,
    private val repository: TourismContentRepository,
) {
    @Transactional
    fun syncAll(): Int {
        val contents = repository.findAll()
        var updatedCount = 0
        contents.forEach { content ->
            val detail = tourApiClient.getCommonDetail(content.contentId) ?: return@forEach
            content.updateCommonDetail(
                telephoneName = detail.telname.nullIfBlank(),
                homepage = detail.homepage.nullIfBlank(),
                bookTour = detail.booktour.nullIfBlank(),
                overview = detail.overview.nullIfBlank(),
            )
            updatedCount++
        }
        repository.saveAll(contents)
        return updatedCount
    }

    private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)
}
