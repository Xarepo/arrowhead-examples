package eu.arrowhead.core.temperatureprovider;

import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;

public class TemperatureProvider {
    public static void main(final String[] args) {
        if (args.length != 6) {
            System.err.println(
                "Usage: java -jar example.jar " +
                    "<keyStorePath>" +
                    "<trustStorePath> " +
                    "<serviceRegistryHostname> " +
                    "<serviceRegistryPort>" +
                    "<localPort>" +
                    "<baseTemperature>"
            );
            System.exit(1);
        }

        final int baseTemperature = Integer.parseInt(args[5]);

        Thermometer thermometer = new Thermometer(baseTemperature);
        TemperatureChart chart = new TemperatureChart("Temperature", thermometer);

        thermometer.start();
        chart.start();

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

            final var system = new ArSystem.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .localHostnamePort("localhost", localPort)
                .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(srSocketAddress))
                .build();

            String uniqueIdentifier = args[4];

            system.provide(new MonitorableService().getService(uniqueIdentifier))
                .ifSuccess(result -> {
                    System.out.println("Providing monitorable service...");
                })
                .onFailure(Throwable::printStackTrace);

            system.provide(new TemperatureService().getService(thermometer))
                .ifSuccess(result -> {
                    System.out.println("Providing temperature service...");
                })
                .onFailure(Throwable::printStackTrace);

        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
