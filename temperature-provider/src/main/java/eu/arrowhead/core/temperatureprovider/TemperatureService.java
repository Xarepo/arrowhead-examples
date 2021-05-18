package eu.arrowhead.core.temperatureprovider;

import eu.arrowhead.core.common.TemperatureDto;
import se.arkalix.codec.CodecType;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

public class TemperatureService {

    public HttpService getService(Thermometer thermometer) {

        return new HttpService()
            .name("temperature")
            .codecs(CodecType.JSON)
            .accessPolicy(AccessPolicy.cloud())
            .basePath("/temperature")
            .get("/temp", (request, response) -> {
                double temperature = thermometer.getTemperature();
                System.out.println("Sending temperature " +
                    temperature + " to " +
                    request.consumer().identity().name());
                TemperatureDto tempDto = new TemperatureDto.Builder()
                    .celsius(temperature)
                    .build();
                response
                    .status(HttpStatus.OK)
                    .body(tempDto, CodecType.JSON);

                return Future.done();
            });
    }

}
