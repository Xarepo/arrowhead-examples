package eu.arrowhead.core.common;

import se.arkalix.codec.CodecType;
import se.arkalix.codec.json.JsonObject;
import se.arkalix.codec.json.JsonPair;
import se.arkalix.codec.json.JsonString;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class MonitorableService {

    public HttpService getService(
        final Map<String, String> metadata,
        final String uniqueIdentifier
    ) {
        Objects.requireNonNull(uniqueIdentifier, "Expected unique identifier");

        return new HttpService()
            .name("monitorable")
            .metadata(metadata)
            .codecs(CodecType.JSON)
            .accessPolicy(AccessPolicy.cloud())
            .basePath("/" + uniqueIdentifier + "/monitorable")
            .get("/ping", (request, response) -> {
                System.out.println("Handling a ping request!");
                PingDto ping = new PingDto.Builder()
                    .ping(true)
                    .build();
                response
                    .status(HttpStatus.OK)
                    .body(ping, CodecType.JSON);

                return Future.done();
            })
            .get("/inventoryid", (request, response) -> {
                System.out.println("Handling a inventory ID request!");
                InventoryIdDto id = new InventoryIdDto.Builder()
                    .id(uniqueIdentifier)
                    .build();
                response
                    .status(HttpStatus.OK)
                    .body(id, CodecType.JSON);

                return Future.done();
            })
            .get("/systemdata", (request, response) -> {
                System.out.println("Handling a system data request!");
                JsonObject data = new JsonObject(new JsonPair("uniqueIdentifier", new JsonString(uniqueIdentifier)));
                SystemDataDto ping = new SystemDataDto.Builder()
                    .data(data)
                    .build();
                response
                    .status(HttpStatus.OK)
                    .body(ping, CodecType.JSON);

                return Future.done();
            });
    }

    public HttpService getService(final String uniqueIdentifier) {
        return getService(Collections.emptyMap(), uniqueIdentifier);
    }
}
