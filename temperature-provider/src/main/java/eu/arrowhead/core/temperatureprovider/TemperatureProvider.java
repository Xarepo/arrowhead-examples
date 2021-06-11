package eu.arrowhead.core.temperatureprovider;

import eu.arrowhead.core.common.Metadata;
import eu.arrowhead.core.common.MonitorableService;
import eu.arrowhead.core.common.Props;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public class TemperatureProvider {
    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar example.jar <path to config file>");
            System.exit(1);
        }

        try {

            final Props props = new Props();
            props.load(args[0]);

            final int baseTemperature = props.getInt(PropNames.BASE_TEMPERATURE);

            final Thermometer thermometer = new Thermometer(baseTemperature);
            TemperatureChart chart = new TemperatureChart("Temperature", thermometer);

            thermometer.start();
            chart.start();

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

            String uniqueIdentifier = String.valueOf(baseTemperature);
            final Map<String, String> systemMetadata = Metadata.getSystemMetadata(uniqueIdentifier);

            final ArSystem system = new ArSystem.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .metadata(systemMetadata)
                .localHostnamePort(localHostname, localPort)
                .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(srSocketAddress))
                .build();

            final Map<String, String> metadata = Map.of("baseTemp", uniqueIdentifier);

            system.provide(new MonitorableService().getService(uniqueIdentifier))
                .ifSuccess(result -> System.out.println("Providing monitorable service..."))
                .onFailure(Throwable::printStackTrace);

            system.provide(new TemperatureService().getService(thermometer, metadata))
                .ifSuccess(result -> System.out.println("Providing temperature service..."))
                .onFailure(Throwable::printStackTrace);

        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
