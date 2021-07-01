package eu.arrowhead.core.pdetester;

import eu.arrowhead.core.common.Metadata;
import eu.arrowhead.core.pdetester.dto.PlantDescriptionEntryDto;
import eu.arrowhead.core.pdetester.dto.PortEntry;
import eu.arrowhead.core.pdetester.dto.SystemEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdeTester {

    private static final Logger logger = LoggerFactory.getLogger(PdeTester.class);
    final int retryDelayMillis = 500;
    final int maxRetries = 100;
    final String retryMessage = "Retrying";
    final RetryFuture retrier = new RetryFuture(retryDelayMillis, maxRetries, retryMessage);
    private final String TEMPERATURE_SERVICE = "temperature";
    private final InetSocketAddress pdeAddress;
    private final HttpClient httpClient;
    private final ArSystem system;
    private final String system1Id = "dp1";
    private final String portNameA = "port-a";
    private final String portNameB = "port-b";
    private final Map<String, String> serviceMetadataA = Metadata.getServiceMetadata("a");
    private final Map<String, String> serviceMetadataB = Metadata.getServiceMetadata("b");
    private final Map<String, String> systemMetadata1 = Metadata.getSystemMetadata("1");
    private final Map<String, String> systemMetadata2 = Metadata.getSystemMetadata("2");

    public PdeTester(final ArSystem system, final HttpClient httpClient, final InetSocketAddress pdeAddress) {
        this.system = Objects.requireNonNull(system, "Expected Arrowhead system");
        this.httpClient = Objects.requireNonNull(httpClient, "Expected HTTP client");
        this.pdeAddress = Objects.requireNonNull(pdeAddress, "Expected PDE address.");
    }

    public Future<Void> start() {
        try {
            return putPlantDescription(PdFiles.NO_CONNECTIONS)
                .flatMap(result -> assertNoServices())
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_SYS_1_USING_PROVIDER_SYSTEM_NAME))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertBothSystem1Services);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_SYS_2_USING_PROVIDER_SYSTEM_METADATA))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertBothSystem2Services);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_BOTH_USING_PROVIDER_SYSTEM_METADATA))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertAllServices);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_SYS_1_SERVICE_A))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertSystem1ServiceA);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_SYS_2_USING_CONSUMER_SYSTEM_METADATA))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertBothSystem2Services);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_SYS_2_SERVICE_B_USING_CONSUMER_SYSTEM_METADATA))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertSystem2ServiceB);
                })
                .flatMap(result -> putPlantDescription(PdFiles.CONNECT_TO_B_SERVICES))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertBothBServices);
                })
                .flatMap(result -> putPlantDescription(PdFiles.MONITOR_SERVICE_1A))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::assertMonitorInfoOnSystem1Ports);
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

    private Future<Void> assertSystem2ServiceB() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(1, services.size());
                assertServicePresent(services, systemMetadata2, serviceMetadataB);
                return Future.done();
            });
    }

    private Future<Void> assertBothBServices() {
        return queryServices()
            .flatMap(services -> {
                assertEquals(2, services.size());
                assertServicePresent(services, systemMetadata1, serviceMetadataB);
                assertServicePresent(services, systemMetadata2, serviceMetadataB);
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

    private Future<Void> assertMonitorInfoOnSystem1Ports() {
        return getPlantDescription()
            .flatMap(response -> response.bodyToIfSuccess(PlantDescriptionEntryDto::decodeJson))
            .map(entry -> {
                final SystemEntry sys1 = getSystemById(entry.systems(), system1Id);
                assertNotNull(sys1);
                final PortEntry port1A = getPortByName(sys1.ports(), portNameA);
                final PortEntry port1B = getPortByName(sys1.ports(), portNameB);
                assertNotNull(port1A);
                assertNotNull(port1B);
                assertEquals("a", port1A.inventoryId().orElse(null));
                assertEquals("b", port1B.inventoryId().orElse(null));
                assertEquals("{[uniqueIdentifier: a]}", port1A.systemData().orElse(null) + "");
                assertEquals("{[uniqueIdentifier: b]}", port1B.systemData().orElse(null) + "");
                return null;
            });
    }

    private PortEntry getPortByName(List<PortEntry> ports, String portName) {
        return ports.stream()
            .filter(port -> port.portName().equals(portName))
            .findFirst()
            .orElse(null);
    }

    private SystemEntry getSystemById(final List<SystemEntry> systems, final String id) {
        return systems.stream()
            .filter(system -> system.systemId().equals(id))
            .findFirst()
            .orElse(null);
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

    private Future<HttpClientResponse> getPlantDescription() {
        return httpClient.send(pdeAddress, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/pde/monitor/pd/0")
            .header("accept", "application/json"));
    }

    private String readStringFromFile(final String filename) throws IOException {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);

        if (inputStream == null) {
            throw new IOException("Failed to read file " + filename);
        }

        final InputStreamReader isReader = new InputStreamReader(inputStream);
        final BufferedReader reader = new BufferedReader(isReader);
        final StringBuilder stringBuilder = new StringBuilder();
        String str;
        while ((str = reader.readLine()) != null) {
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }

}
