package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity

@Entity
@Table(name = "tourism_content_images")
class TourismContentImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tourism_content_id", nullable = false, updatable = false)
    val tourismContent: TourismContent,
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    val type: TourismContentImageType,
    @Column(name = "image_name", length = 500)
    val imageName: String?,
    @Column(name = "original_image_url", length = 1000)
    var originalImageUrl: String?,
    @Column(name = "serial_number", length = 100)
    val serialNumber: String?,
    @Column(name = "copyright_type", length = 20)
    val copyrightType: String?,
) : BaseModifiableEntity() {
    fun updateOriginalImageUrl(originalImageUrl: String) {
        this.originalImageUrl = originalImageUrl
    }
}

enum class TourismContentImageType {
    CONTENT,
    MENU,
}
