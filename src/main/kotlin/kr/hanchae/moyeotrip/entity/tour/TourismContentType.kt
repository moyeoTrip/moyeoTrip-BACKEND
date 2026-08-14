package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tourism_content_types")
class TourismContentType(
    @Id
    @Column(name = "code")
    val code: Int,
    @Column(name = "name", nullable = false, length = 30)
    val name: String,
)
