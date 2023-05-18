package com.utkucuval.weather.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utkucuval.weather.constants.Constants;
import com.utkucuval.weather.dto.WeatherDto;
import com.utkucuval.weather.dto.WeatherResponse;
import com.utkucuval.weather.model.WeatherEntity;
import com.utkucuval.weather.repository.WeatherRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@CacheConfig(cacheNames = {"weathers"})
public class WeatherService {


    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeatherRepository weatherRepository;

    private final RestTemplate restTemplate;

    public WeatherService(WeatherRepository weatherRepository, RestTemplate restTemplate) {
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
    }

    @Cacheable(key = "#p0" )
    public WeatherDto getWeatherByCityName(String cityName){
        logger.info("NEW CREATED");
    Optional<WeatherEntity> weatherEntityOptional = weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(cityName);


    return weatherEntityOptional.map(weather->{
        if(weather.getUpdatedTime().isBefore(LocalDateTime.now().minusMinutes(30))){
            return WeatherDto.convert(getWeatherFromWeatherStack(cityName));
        }
        return WeatherDto.convert(weather);
    }) . orElseGet(()->WeatherDto.convert(getWeatherFromWeatherStack(cityName)));
    }


    private WeatherEntity getWeatherFromWeatherStack(String cityName){
        String url = getWeatherStackUrl(cityName);
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(url ,String.class);

        try {
            WeatherResponse weatherResponse = objectMapper.readValue(responseEntity.getBody(),WeatherResponse.class);
            return saveWeatherEntity(cityName,weatherResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


    }
    @CacheEvict(allEntries = true)
    @PostConstruct
    @Scheduled(fixedRateString = "10000")
    public void clearCache(){logger.info("CLEARED");}
    private WeatherEntity saveWeatherEntity(String cityName , WeatherResponse weatherResponse){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        WeatherEntity weatherEntity = new WeatherEntity(
                cityName,
                weatherResponse.location().name(),
                weatherResponse.location().country(),
                weatherResponse.current().temperature(),
                LocalDateTime.now(),
                LocalDateTime.parse(weatherResponse.location().localtime(),dateTimeFormatter));

            return weatherRepository.save(weatherEntity);
    }



    private String getWeatherStackUrl(String cityName){
        return Constants.API_URL + Constants.ACCESS_KEY_PARAM + Constants.API_KEY + Constants.QUERY_KEY + cityName;
    }
}
