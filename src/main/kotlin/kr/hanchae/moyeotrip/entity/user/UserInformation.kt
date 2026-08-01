package kr.hanchae.moyeotrip.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDate

@Embeddable
data class UserInformation(
    @Column(length = 24, unique = true, updatable = false, nullable = true)
    var nickname: String,
    @Column(length = 20, updatable = false, nullable = true)
    @Enumerated(EnumType.STRING)
    var nicknameColor: NicknameColor,
    @Column(updatable = false, nullable = true)
    @Enumerated(EnumType.STRING)
    var gender: Gender,
    @Column(updatable = false, nullable = true)
    var birthDate: LocalDate? = null,
    @Column(nullable = true)
    var profileFileName: String? = null,
)
