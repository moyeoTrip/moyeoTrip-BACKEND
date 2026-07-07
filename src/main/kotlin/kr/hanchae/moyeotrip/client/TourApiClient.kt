package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TourApiClient(
    tourApiProperties: TourApiProperties,
) {
    private val restClient: RestClient =
        RestClient
            .builder()
            .baseUrl("https://apis.data.go.kr")
            .build()
}
