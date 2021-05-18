package eu.arrowhead.core.common;

import se.arkalix.codec.CodecType;
import se.arkalix.codec.json.JsonObject;
import se.arkalix.codec.json.JsonPair;
import se.arkalix.codec.json.JsonString;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.util.concurrent.Future;

import java.util.Map;

public class MonitorableService {

    public HttpService getService(String uniqueIdentifier) {

        Map<String, String> metadata = Map.of("serviceLevel", uniqueIdentifier);
        return new HttpService()
            .name("monitorable")
            .metadata(metadata)
            .codecs(CodecType.JSON)
            .accessPolicy(AccessPolicy.cloud())
            .basePath("/monitorable")
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

}
