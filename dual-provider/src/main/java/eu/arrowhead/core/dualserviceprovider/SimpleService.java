package eu.arrowhead.core.dualserviceprovider;

import java.util.Map;
import eu.arrowhead.core.common.Metadata;
import eu.arrowhead.core.common.TemperatureDto;
import se.arkalix.codec.CodecType;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

public class SimpleService {

    public HttpService getService(final String uid) {

        final Map<String, String> metadata = Metadata.getServiceMetadata(uid);
        return new HttpService()
            .name("temperature")
            .codecs(CodecType.JSON)
            .accessPolicy(AccessPolicy.cloud())
            .basePath("/temperature-" + uid)
            .metadata(metadata)
            .get("/temp", (request, response) -> {
                TemperatureDto tempDto = new TemperatureDto.Builder()
                    .celsius(42)
                    .build();
                response
                    .status(HttpStatus.OK)
                    .body(tempDto, CodecType.JSON);
                return Future.done();
            });
    }

}
