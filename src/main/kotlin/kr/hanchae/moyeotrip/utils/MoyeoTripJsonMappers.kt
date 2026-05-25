package kr.hanchae.moyeotrip.utils

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object MoyeoTripJsonMappers {
    val default: JsonMapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build()
}
