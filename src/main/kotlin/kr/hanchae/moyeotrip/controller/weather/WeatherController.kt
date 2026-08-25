package kr.hanchae.moyeotrip.controller.weather

import kr.hanchae.moyeotrip.controller.weather.response.GyeongbukWeatherResponse
import kr.hanchae.moyeotrip.service.weather.WeatherService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/weather")
class WeatherController(
    private val weatherService: WeatherService,
) : WeatherAPISpec {
    @GetMapping("/gyeongbuk")
    override fun getGyeongbukWeather(): GyeongbukWeatherResponse = weatherService.getGyeongbukWeather()
}
