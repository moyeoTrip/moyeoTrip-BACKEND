package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity

@Entity
@Table(name = "tourism_content_image_sync_progress")
class TourismContentImageSyncProgress(
    @Id
    @Column(name = "sync_name", length = 50)
    val syncName: String,
    @Column(name = "last_content_id")
    var lastContentId: Long? = null,
    @Column(nullable = false, columnDefinition = "NUMBER(1)")
    var completed: Boolean = false,
) : BaseModifiableEntity() {
    fun advanceTo(contentId: Long) {
        lastContentId = contentId
    }

    fun complete() {
        completed = true
    }
}
