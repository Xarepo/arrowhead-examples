package eu.arrowhead.core.temperatureprovider;

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
                System.out.println("Handling a temperature request!");
                TempDto temp = new TempDto.Builder()
                    .celsius(thermometer.getTemperature())
                    .build();
                response
                    .status(HttpStatus.OK)
                    .body(temp, CodecType.JSON);

                return Future.done();
            });
    }

}
