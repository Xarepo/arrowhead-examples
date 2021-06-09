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
import se.arkalix.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.arrowhead.core.common.Metadata;

public class PdeTester {

    private static final Logger logger = LoggerFactory.getLogger(PdeTester.class);

    private final String TEMPERATURE_SERVICE = "temperature";
    private final InetSocketAddress pdeAddress;

    private final HttpClient httpClient;
    private final ArSystem system;

    private final Map<String, String> serviceMetadataA = Metadata.getServiceMetadata("a");
    private final Map<String, String> serviceMetadataB = Metadata.getServiceMetadata("b");
    private final Map<String, String> systemMetadata1 = Metadata.getSystemMetadata("1");
    private final Map<String, String> systemMetadata2 = Metadata.getSystemMetadata("2");

    final int retryDelayMillis = 500;
    final int maxRetries = 10;
    final String retryMessage = "Retrying";

    final RetryFuture retrier = new RetryFuture(retryDelayMillis, maxRetries, retryMessage);

    public PdeTester(final ArSystem system, final HttpClient httpClient, final InetSocketAddress pdeAddress) {
        this.system = Objects.requireNonNull(system, "Expected Arrowhead system");
        this.httpClient = Objects.requireNonNull(httpClient, "Expected HTTP client");
        this.pdeAddress = Objects.requireNonNull(pdeAddress, "Expected PDE address.");
    }

    public Future<Void> start() {
        try {
            return putPlantDescription(PdFiles.NO_CONNECTIONS)
                .flatMap(result -> assertNoServices())
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_TEMP_1_USING_SYSTEM_NAME))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertBothSystem1Services);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_TEMP_2_USING_SYSTEM_METADATA))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertBothSystem2Services);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_BOTH_USING_SYSTEM_METADATA))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertAllServices);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_SYSTEM_1_SERVICE_A))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertSystem1ServiceA);
                });

        } catch (IOException e) {
            return Future.failure(e);
        }
    }

    private Future<Void> assertNoServices() {
        return queryServices()
            .flatMap(services -> {
                assertTrue(services.isEmpty());
                logger.info("No services available.");
                return Future.done();
            });
    }

    private Future<Void> assertBothSystem1Services() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(2, services.size());
                assertServicePresent(services, systemMetadata1, serviceMetadataA);
                assertServicePresent(services, systemMetadata1, serviceMetadataB);
                return Future.done();
            });
    }

    private Future<Void> assertBothSystem2Services() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(2, services.size());
                assertServicePresent(services, systemMetadata2, serviceMetadataA);
                assertServicePresent(services, systemMetadata2, serviceMetadataB);
                return Future.done();
            });
    }

    private Future<Void> assertAllServices() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(4, services.size());
                assertServicePresent(services, systemMetadata1, serviceMetadataA);
                assertServicePresent(services, systemMetadata1, serviceMetadataB);
                assertServicePresent(services, systemMetadata2, serviceMetadataA);
                assertServicePresent(services, systemMetadata2, serviceMetadataB);
                return Future.done();
            });
    }

    private Future<Void> assertSystem1ServiceA() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(1, services.size());
                assertServicePresent(services, systemMetadata1, serviceMetadataA);
                return Future.done();
            });
    }

    private void assertServicePresent(
        Set<ServiceRecord> services,
        Map<String, String> systemMetadata,
        Map<String, String> serviceMetadata
    ) {
        ServiceRecord match = services.stream()
            .filter(service -> matches(service, systemMetadata, serviceMetadata))
            .findFirst()
            .orElse(null);
        assertNotNull(match);
    }

    private boolean matches(
        final ServiceRecord service,
        final Map<String, String> systemMetadata,
        final Map<String, String> serviceMetadata
        ) {
            return systemMetadata.equals(service.provider().metadata()) && serviceMetadata.equals(service.metadata());
    }

    private Future<Set<ServiceRecord>> queryServices() {
        return system.consume()
            .name(TEMPERATURE_SERVICE)
            .codecTypes(CodecType.JSON)
            .protocolTypes(ProtocolType.HTTP)
            .resolveAll();
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
