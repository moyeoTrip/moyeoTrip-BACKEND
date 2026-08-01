package kr.hanchae.moyeotrip.controller.auth.response

import kr.hanchae.moyeotrip.entity.user.ProviderType

data class LinkedProvidersResponse(
    val providers: Set<ProviderType>,
)
