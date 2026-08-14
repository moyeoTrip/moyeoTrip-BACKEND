package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity

@Entity
@Table(name = "tourism_content_details")
class TourismContentDetail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tourism_content_id", nullable = false, updatable = false, unique = true)
    val tourismContent: TourismContent,
    @Lob
    @Column(name = "intro_payload", nullable = false, columnDefinition = "CLOB")
    val introPayload: String,
    @Lob
    @Column(name = "info_payload", nullable = false, columnDefinition = "CLOB")
    val infoPayload: String,
    @Lob
    @Column(name = "content_image_payload", nullable = false, columnDefinition = "CLOB")
    val contentImagePayload: String,
    @Lob
    @Column(name = "menu_image_payload", nullable = false, columnDefinition = "CLOB")
    val menuImagePayload: String,
) : BaseModifiableEntity()
