package kr.hanchae.moyeotrip.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class UserInformation(
    @Column(length = 24, unique = true, updatable = false, nullable = true)
    var nickname: String,
    @Column(updatable = false, nullable = true)
    @Enumerated(EnumType.STRING)
    var gender: Gender,
    @Column(updatable = false, nullable = true)
    var profileFileName: String? = null,
)
