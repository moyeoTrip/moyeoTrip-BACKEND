package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import java.time.LocalDateTime

@Entity
@Table(
    name = "tourism_contents",
    uniqueConstraints = [UniqueConstraint(name = "uk_tourism_content_id", columnNames = ["content_id"])],
)
class TourismContent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "content_id", nullable = false)
    val contentId: Long,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_type_id", nullable = false)
    var contentType: TourismContentType,
    @Column(nullable = false, length = 300)
    var title: String,
    @Column(length = 500)
    var address1: String? = null,
    @Column(length = 500)
    var address2: String? = null,
    @Column(length = 10)
    var zipcode: String? = null,
    @Column(length = 300)
    var telephone: String? = null,
    @Column(name = "telephone_name", length = 300)
    var telephoneName: String? = null,
    @Column(name = "homepage", length = 2000)
    var homepage: String? = null,
    @Lob
    @Column(name = "overview", columnDefinition = "CLOB")
    var overview: String? = null,
    @Column(name = "thumbnail", length = 1000)
    var thumbnail: String? = null,
    @Column(name = "copyright_type", length = 20)
    var copyrightType: String? = null,
    @Column(name = "longitude")
    var longitude: Double? = null,
    @Column(name = "latitude")
    var latitude: Double? = null,
    @Column(name = "map_level", length = 10)
    var mapLevel: String? = null,
    @Column(name = "source_created_datetime")
    var sourceCreatedDateTime: LocalDateTime? = null,
    @Column(name = "source_modified_datetime")
    var sourceModifiedDateTime: LocalDateTime? = null,
    @Column(name = "region_code", length = 2)
    var regionCode: String? = null,
    @Column(name = "signgu_code", length = 3)
    var signguCode: String? = null,
    @Column(name = "level1_code", length = 3)
    var level1Code: String? = null,
    @Column(name = "level2_code", length = 5)
    var level2Code: String? = null,
    @Column(name = "level3_code", length = 9)
    var level3Code: String? = null,
) : BaseModifiableEntity() {
    fun updateCommonDetail(
        telephoneName: String?,
        homepage: String?,
        overview: String?,
    ) {
        this.telephoneName = telephoneName
        this.homepage = homepage
        this.overview = overview
    }

    fun updateThumbnail(thumbnail: String?) {
        this.thumbnail = thumbnail
    }

    fun update(
        contentType: TourismContentType,
        title: String,
        address1: String?,
        address2: String?,
        zipcode: String?,
        telephone: String?,
        thumbnail: String?,
        copyrightType: String?,
        longitude: Double?,
        latitude: Double?,
        mapLevel: String?,
        sourceCreatedDateTime: LocalDateTime?,
        sourceModifiedDateTime: LocalDateTime?,
        regionCode: String?,
        signguCode: String?,
        level1Code: String?,
        level2Code: String?,
        level3Code: String?,
    ) {
        this.contentType = contentType
        this.title = title
        this.address1 = address1
        this.address2 = address2
        this.zipcode = zipcode
        this.telephone = telephone
        this.thumbnail = thumbnail
        this.copyrightType = copyrightType
        this.longitude = longitude
        this.latitude = latitude
        this.mapLevel = mapLevel
        this.sourceCreatedDateTime = sourceCreatedDateTime
        this.sourceModifiedDateTime = sourceModifiedDateTime
        this.regionCode = regionCode
        this.signguCode = signguCode
        this.level1Code = level1Code
        this.level2Code = level2Code
        this.level3Code = level3Code
    }
}
