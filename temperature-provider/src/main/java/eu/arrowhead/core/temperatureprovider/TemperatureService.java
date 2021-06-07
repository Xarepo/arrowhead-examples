package eu.arrowhead.core.temperatureprovider;

import eu.arrowhead.core.common.TemperatureDto;
import se.arkalix.codec.CodecType;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

public class TemperatureService {

    public HttpService getService() {

        return new HttpService()
            .name("temperature")
            .codecs(CodecType.JSON)
            .accessPolicy(AccessPolicy.cloud())
            .basePath("/temperature")
            .get("/temp", (request, response) -> {
                final String recipient = request.consumer().identity().name();
                System.out.println("Sending temperature to " + recipient);
                TemperatureDto tempDto = new TemperatureDto.Builder()
                    .celsius(25)
                    .build();
                response
                    .status(HttpStatus.OK)
                    .body(tempDto, CodecType.JSON);

                return Future.done();
            });
    }

}
