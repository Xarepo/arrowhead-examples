package eu.arrowhead.core.fan;

import eu.arrowhead.core.common.MonitorableService;
import se.arkalix.ArServiceRecordCache;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.or.OrchestrationOption;
import se.arkalix.core.plugin.or.OrchestrationPattern;
import se.arkalix.core.plugin.or.OrchestrationStrategy;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

public class Fan {

    public static void main(final String[] args) {
        if (args.length != 5) {
            System.err.println(
                "Usage: java -jar example.jar " +
                    "<keyStorePath>" +
                    "<trustStorePath> " +
                    "<serviceRegistryHostname> " +
                    "<serviceRegistryPort>" +
                    "<localPort>"
            );
            System.exit(1);
        }

        try {
            // Load owned system identity and truststore.
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var identity = new OwnedIdentity.Loader()
                .keyPassword(password)
                .keyStorePath(Path.of(args[0]))
                .keyStorePassword(password)
                .load();
            final var trustStore = TrustStore.read(Path.of(args[1]), password);
            Arrays.fill(password, '\0');

            final var srSocketAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            final int localPort = Integer.parseInt(args[4]);

            final OrchestrationStrategy strategy = new OrchestrationStrategy(
                new OrchestrationPattern().isIncludingService(true)
                    .option(OrchestrationOption.PING_PROVIDERS, true)
                    .option(OrchestrationOption.OVERRIDE_STORE, false));

            final ArSystem system = new ArSystem.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .localHostnamePort("localhost", localPort)
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

            String uniqueIdentifier = args[4];

            system.provide(new MonitorableService().getService(uniqueIdentifier))
                .ifSuccess(result -> System.out.println("Providing monitorable service..."))
                .onFailure(Throwable::printStackTrace);

        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
