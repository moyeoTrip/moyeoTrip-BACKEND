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
    name = "tour_classification_codes",
    uniqueConstraints = [UniqueConstraint(name = "uk_tour_classification_level3", columnNames = ["level3_code"])],
)
class TourClassificationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "level1_code", nullable = false, length = 3)
    val level1Code: String,
    @Column(name = "level2_code", nullable = false, length = 5)
    val level2Code: String,
    @Column(name = "level3_code", nullable = false, length = 9)
    val level3Code: String,
    @Column(name = "level1_name", nullable = false, length = 100)
    var level1Name: String,
    @Column(name = "level2_name", nullable = false, length = 100)
    var level2Name: String,
    @Column(name = "level3_name", nullable = false, length = 100)
    var level3Name: String,
) : BaseModifiableEntity() {
    fun updateNames(
        level1Name: String,
        level2Name: String,
        level3Name: String,
    ) {
        this.level1Name = level1Name
        this.level2Name = level2Name
        this.level3Name = level3Name
    }
}
