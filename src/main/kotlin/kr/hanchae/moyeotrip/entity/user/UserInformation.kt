package kr.hanchae.moyeotrip.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class UserInformation(
    @Column(unique = true)
    val email: String?,

    @Column(nullable = false, length = 24, unique = true, updatable = false)
    val nickname: String,

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    val gender: Gender?,

    @Column(nullable = false, updatable = false)
    val profileFileName: String? = "DEFAULT",
)
