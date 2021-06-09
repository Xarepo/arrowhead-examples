package eu.arrowhead.core.pdetester;

import se.arkalix.ArServiceRecordCache;
import se.arkalix.ArSystem;
import se.arkalix.codec.CodecType;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.or.OrchestrationOption;
import se.arkalix.core.plugin.or.OrchestrationPattern;
import se.arkalix.core.plugin.or.OrchestrationStrategy;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.arrowhead.core.common.PropertyException;
import eu.arrowhead.core.common.Props;

public class PdeTesterMain {

    private static final Logger logger = LoggerFactory.getLogger(PdeTesterMain.class);

    private static HttpService dummyService() {
        return new HttpService()
            .name("dummy")
            .codecs(CodecType.JSON)
            .accessPolicy(AccessPolicy.cloud())
            .basePath("/dummy")
            .get("/dummy", (request, response) -> {
                response.status(HttpStatus.OK);
                return Future.done();
            });
    }


    public static void main(final String[] args) throws PropertyException {

        if (args.length != 1) {
            System.err.println("Usage: java -jar example.jar <path to config file>");
            System.exit(1);
        }

        final Props props = new Props();
        props.load(args[0]);

        final char[] keyPassword = props.getString(PropNames.KEY_PASSWORD).toCharArray();
        final String keyStorePath = props.getString(PropNames.KEY_STORE);
        final char[] keyStorePassword = props.getString(PropNames.KEY_STORE_PASSWORD).toCharArray();
        final String trustStorePath = props.getString(PropNames.TRUST_STORE);
        final char[] trustStorePassword = props.getString(PropNames.TRUST_STORE_PASSWORD).toCharArray();
        final String localAddress = props.getString(PropNames.LOCAL_HOSTNAME);
        final int localPort = props.getInt(PropNames.LOCAL_PORT);
        final String srAddress = props.getString(PropNames.SR_HOSTNAME);
        final int srPort = props.getInt(PropNames.SR_PORT);

        try {

            final TrustStore trustStore = TrustStore.read(trustStorePath, trustStorePassword);
            final OwnedIdentity identity = new OwnedIdentity.Loader()
                .keyPassword(keyPassword)
                .keyStorePath(keyStorePath)
                .keyStorePassword(keyStorePassword)
                .load();

            Arrays.fill(keyPassword, '\0');
            Arrays.fill(keyStorePassword, '\0');
            Arrays.fill(trustStorePassword, '\0');

            final InetSocketAddress srSocketAddress = new InetSocketAddress(srAddress, srPort);

            final OrchestrationStrategy strategy = new OrchestrationStrategy(
                new OrchestrationPattern().isIncludingService(true)
                    .option(OrchestrationOption.PING_PROVIDERS, true)
                    .option(OrchestrationOption.OVERRIDE_STORE, false));

            final ArSystem system = new ArSystem.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .localHostnamePort(localAddress, localPort)
                .serviceCache(ArServiceRecordCache.withEntryLifetimeLimit(Duration.ZERO))
                .plugins(new HttpJsonCloudPlugin.Builder()
                    .orchestrationStrategy(strategy)
                    .serviceRegistrySocketAddress(srSocketAddress)
                    .build())
                .build();

            final HttpClient httpClient = new HttpClient.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .build();

            final String pdeHostname = props.getString(PropNames.PDE_HOSTNAME);
            final int pdePort = Integer.parseInt(props.getString(PropNames.PDE_PORT));
            final InetSocketAddress pdeAddress = new InetSocketAddress(pdeHostname, pdePort);
            final PdeTester test = new PdeTester(system, httpClient, pdeAddress);

            // Start by registering a dummy service, in order to get this system
            // entered in the service registry:
            system.provide(dummyService())
                .flatMap(result -> test.start())
                .ifSuccess(result -> {
                    logSuccess();
                    System.exit(0);
                })
                .onFailure(e -> {
                    fail(e);
                });

        } catch (final Throwable e) {
            fail(e);
        }
    }

    private static void logSuccess() {
        logger.info(
            "\n" +
            "=======================================\n" +
            "||   The PDE is running correctly.   ||\n" +
            "======================================="
        );
    }

    private static void fail(Throwable e) {
        logger.error("Test failed", e);
        System.exit(1);
    }
}
