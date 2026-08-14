package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity

@Entity
@Table(
    name = "legal_dong_codes",
    uniqueConstraints = [UniqueConstraint(name = "uk_legal_dong_region_signgu", columnNames = ["region_code", "signgu_code"])],
)
class LegalDongCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "region_code", nullable = false, length = 2)
    val regionCode: String,
    @Column(name = "signgu_code", nullable = false, length = 5)
    val signguCode: String,
    @Column(name = "region_name", nullable = false, length = 50)
    var regionName: String,
    @Column(name = "signgu_name", nullable = false, length = 50)
    var signguName: String,
) : BaseModifiableEntity() {
    fun updateNames(
        regionName: String,
        signguName: String,
    ) {
        this.regionName = regionName
        this.signguName = signguName
    }
}
