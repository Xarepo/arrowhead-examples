package eu.arrowhead.core.pdetester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
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

public class PdeTest {

    private static final Logger logger = LoggerFactory.getLogger(PdeTest.class);

    private final String TEMPERATURE_SERVICE = "temperature";
    private InetSocketAddress pdeAddress = new InetSocketAddress("localhost", 28081);

    private final HttpClient httpClient;
    private final ArSystem system;

    private final ThermometerReader thermometerReader1;
    private final ThermometerReader thermometerReader2;

    final int retryDelayMillis = 500;
    final int maxRetries = 10;
    final String retryMessage = "Retrying";

    final RetryFuture retrier = new RetryFuture(retryDelayMillis, maxRetries, retryMessage);

    public PdeTest(
            final ArSystem system,
            final HttpClient httpClient,
            final ThermometerReader thermometerReader1,
            final ThermometerReader thermometerReader2) {
        Objects.requireNonNull(system, "Expected Arrowhead system");
        Objects.requireNonNull(httpClient, "Expected HTTP client");
        Objects.requireNonNull(thermometerReader1,
                "Expected Thermometer Reader as first argument.");
        Objects.requireNonNull(thermometerReader2,
                "Expected Thermometer Reader as second argument.");

        this.system = system;
        this.httpClient = httpClient;
        this.thermometerReader1 = thermometerReader1;
        this.thermometerReader2 = thermometerReader2;
    }

    public Future<Void> start() {
        try {
            return putPlantDescription("pd0.json")
                .flatMap(result -> ensureNoServicesAvailable())
                .flatMap(result -> putPlantDescription("pd1.json"))
                .flatMap(response -> {
                    assertEquals(HttpStatus.OK, response.status());
                    return retrier.run(this::ensureServicesAvailable);
                });
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

    private Future<Void> ensureServicesAvailable() {
        return queryServices()
            .flatMap(services -> {
                assertFalse(services.isEmpty());
                logger.info("Found avaialable services.");
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

    private ServiceQuery getPdeManagementServiceQuery() {
        return system.consume()
            .name("pde-mgmt")
            .codecTypes(CodecType.JSON)
            .protocolTypes(ProtocolType.HTTP);
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
