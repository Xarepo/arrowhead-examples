package eu.arrowhead.core.fan;

import eu.arrowhead.core.common.Props;
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
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

public class Fan {

    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar example.jar <path to config file>");
            System.exit(1);
        }

        try {

            final Props props = new Props();
            props.load(args[0]);

            final String keyStorePath = props.getString(PropNames.KEY_STORE);
            final char[] keyPassword = props.getString(PropNames.KEY_PASSWORD).toCharArray();
            final char[] keyStorePassword = props.getString(PropNames.KEY_STORE_PASSWORD).toCharArray();
            final OwnedIdentity identity = new OwnedIdentity.Loader()
                .keyPassword(keyPassword)
                .keyStorePath(Path.of(keyStorePath))
                .keyStorePassword(keyStorePassword)
                .load();

            final String trustStorePath = props.getString(PropNames.TRUST_STORE);
            final char[] trustStorePassword = props.getString(PropNames.TRUST_STORE_PASSWORD).toCharArray();

            final TrustStore trustStore = TrustStore.read(trustStorePath, trustStorePassword);

            Arrays.fill(keyPassword, '\0');
            Arrays.fill(keyStorePassword, '\0');
            Arrays.fill(trustStorePassword, '\0');

            final String localHostname = props.getString(PropNames.LOCAL_HOSTNAME);
            final int localPort = props.getInt(PropNames.LOCAL_PORT);

            final String srHostname = props.getString(PropNames.SR_HOSTNAME);
            final int srPort = props.getInt(PropNames.SR_PORT);
            final InetSocketAddress srSocketAddress = new InetSocketAddress(srHostname, srPort);

            final OrchestrationStrategy strategy = new OrchestrationStrategy(
                new OrchestrationPattern().isIncludingService(true)
                    .option(OrchestrationOption.PING_PROVIDERS, true)
                    .option(OrchestrationOption.OVERRIDE_STORE, false));

            final ArSystem system = new ArSystem.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .localHostnamePort(localHostname, localPort)
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

            final ThermometerReader thermometerReader = new ThermometerReader(system, httpClient);
            thermometerReader.start();

            FanAnimation frame = new FanAnimation(thermometerReader);
            frame.init();

            final var pingService = new HttpService()
                .name("ping")
                .codecs(CodecType.JSON)
                .accessPolicy(AccessPolicy.cloud())
                .basePath("/")
                .get("/ping", (request, response) -> {
                    response
                        .status(HttpStatus.OK)
                        .body("ok");
                    return Future.done();
                });

            system.provide(pingService)
                .ifSuccess(result -> System.out.println("Providing ping service..."))
                .onFailure(Throwable::printStackTrace);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
