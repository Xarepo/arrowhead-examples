package eu.arrowhead.core.pdetester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import se.arkalix.ArSystem;
import se.arkalix.ServiceRecord;
import se.arkalix.codec.CodecType;
import se.arkalix.net.ProtocolType;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.net.http.client.HttpClientResponse;
import se.arkalix.query.ServiceQuery;
import se.arkalix.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.arrowhead.core.common.Metadata;

public class PdeTest {

    private static final Logger logger = LoggerFactory.getLogger(PdeTest.class);

    private final String TEMPERATURE_SERVICE = "temperature";
    private final InetSocketAddress pdeAddress;

    private final HttpClient httpClient;
    private final ArSystem system;

    private final String uid1 = "9001";
    private final String uid2 = "9002";

    private final Map<String, String> serviceMetadata1 = Metadata.getServiceMetadata(uid1);
    private final Map<String, String> serviceMetadata2 = Metadata.getServiceMetadata(uid2);
    private final Map<String, String> systemMetadata1 = Metadata.getSystemMetadata(uid1);
    private final Map<String, String> systemMetadata2 = Metadata.getSystemMetadata(uid2);


    final int retryDelayMillis = 500;
    final int maxRetries = 10;
    final String retryMessage = "Retrying";

    final RetryFuture retrier = new RetryFuture(retryDelayMillis, maxRetries, retryMessage);

    public PdeTest(final ArSystem system, final HttpClient httpClient, final InetSocketAddress pdeAddress) {
        this.system = Objects.requireNonNull(system, "Expected Arrowhead system");
        this.httpClient = Objects.requireNonNull(httpClient, "Expected HTTP client");
        this.pdeAddress = Objects.requireNonNull(pdeAddress, "Expected PDE address.");
    }

    public Future<Void> start() {
        try {
            return putPlantDescription(PdFiles.NO_CONNECTIONS)
                .flatMap(result -> ensureNoServicesAvailable())
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_TEMP_1_USING_NAME))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::ensureService1Only);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_TEMP_2_USING_SYSTEM_METADATA))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::ensureService2Only);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_BOTH_USING_SYSTEM_METADATA)
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::ensureBothServices);
                }));

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Future<Void> ensureNoServicesAvailable() {
        return queryServices()
            .flatMap(services -> {
                assertTrue(services.isEmpty());
                logger.info("No services available.");
                return Future.done();
            });
    }

    private Future<Void> ensureService1Only() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(1, services.size());
                ServiceRecord temp1Service = services.stream().findFirst().orElse(null);
                assertEquals(systemMetadata1, temp1Service.provider().metadata());
                logger.info("Connected to service 1.");
                return Future.done();
            });
    }

    private Future<Void> ensureService2Only() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(1, services.size());
                ServiceRecord temp1Service = services.stream().findFirst().orElse(null);
                assertEquals(systemMetadata2, temp1Service.provider().metadata());
                logger.info("Connected to service 1.");
                return Future.done();
            });
    }

    private Future<Void> ensureBothServices() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(2, services.size());
                ServiceRecord temp1Service = services.stream()
                    .filter(service -> service.provider().metadata().equals(systemMetadata1))
                    .findFirst()
                    .orElse(null);
                ServiceRecord temp2Service = services.stream()
                    .filter(service -> service.provider().metadata().equals(systemMetadata2))
                    .findFirst()
                    .orElse(null);

                assertNotNull(temp1Service);
                assertNotNull(temp2Service);
                logger.info("Connected to both services.");
                return Future.done();
            });
    }


    private Future<Set<ServiceRecord>> queryServices() {
        return getServiceQuery()
            .resolveAll();
    }

    private ServiceQuery getServiceQuery() {
        return system.consume()
            .name(TEMPERATURE_SERVICE)
            .codecTypes(CodecType.JSON)
            .protocolTypes(ProtocolType.HTTP);
    }

    private Future<HttpClientResponse> putPlantDescription(String filename) throws IOException {
        final String plantDescription = readStringFromFile(filename);
        return httpClient.send(pdeAddress, new HttpClientRequest()
            .method(HttpMethod.PUT)
            .uri("/pde/mgmt/pd/0")
            .body(plantDescription, Charset.defaultCharset())
            .header("accept", "application/json"));
    }

    private String readStringFromFile(final String filename) throws IOException {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
        final InputStreamReader isReader = new InputStreamReader(inputStream);
        final BufferedReader reader = new BufferedReader(isReader);
        final StringBuffer stringBuffer = new StringBuffer();
        String str;
        while ((str = reader.readLine()) != null) {
            stringBuffer.append(str);
        }
        return stringBuffer.toString();
    }

}
