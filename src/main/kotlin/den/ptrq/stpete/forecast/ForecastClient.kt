package den.ptrq.stpete.forecast

import com.fasterxml.jackson.annotation.JsonProperty
import den.ptrq.stpete.MockableInTests
import den.ptrq.stpete.get
import org.springframework.web.client.RestTemplate

/**
 * @author petrique
 */
@MockableInTests
class ForecastClient(
    private val restTemplate: RestTemplate,
    token: String
) {
    private val cityId = 498817
    private val url = "https://api.openweathermap.org/data/2.5/forecast?APPID=$token&id=$cityId&units=metric"

    fun getForecast(): ForecastResponse {
        return restTemplate
            .get<ForecastResponse>(url)
            .getValueOrElse { throw RuntimeException("forecast call failed with error ${getError()}") }
    }
}

class ForecastResponse(
    @JsonProperty("cod") val code: String,
    @JsonProperty("list") val forecastItems: List<ForecastItem>
)

class ForecastItem(
    @JsonProperty("dt") val date: Long,
    @JsonProperty("clouds") val clouds: Clouds
)

class Clouds(@JsonProperty("all") val percentage: Int)
