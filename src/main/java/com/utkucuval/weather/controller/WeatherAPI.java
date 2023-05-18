package com.utkucuval.weather.controller;

import com.utkucuval.weather.controller.validation.CityNameConstraint;
import com.utkucuval.weather.dto.WeatherDto;
import com.utkucuval.weather.service.WeatherService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/weather")
@Validated
public class WeatherAPI {

    private final WeatherService weatherService;

    public WeatherAPI(WeatherService weatherService) {
        this.weatherService = weatherService;
    }
    @GetMapping("/{city}")
    @RateLimiter(name="basic")
    public ResponseEntity<WeatherDto> getWeather(@PathVariable("city") @CityNameConstraint  @NotBlank String cityName ){
        return ResponseEntity.ok(weatherService.getWeatherByCityName(cityName));
    }
}
