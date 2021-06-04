package eu.arrowhead.core.dualserviceprovider;

import eu.arrowhead.core.common.Metadata;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public class DualServiceProvider {
    public static void main(final String[] args) {
        if (args.length != 6) {
            System.err.println(
                "Usage: java -jar example.jar " +
                    "<keyStorePath>" +
                    "<trustStorePath> " +
                    "<serviceRegistryHostname> " +
                    "<serviceRegistryPort>" +
                    "<localPort>" +
                    "<uid>"
            );
            System.exit(1);
        }

        try {
            final char[] password = new char[]{'1', '2', '3', '4', '5', '6'};
            final OwnedIdentity identity = new OwnedIdentity.Loader()
                .keyPassword(password)
                .keyStorePath(Path.of(args[0]))
                .keyStorePassword(password)
                .load();
            final TrustStore trustStore = TrustStore.read(Path.of(args[1]), password);
            Arrays.fill(password, '\0');

            final InetSocketAddress srSocketAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            final int localPort = Integer.parseInt(args[4]);

            final String uid = args[5];
            final Map<String, String> systemMetadata = Metadata.getSystemMetadata(uid);

            final ArSystem system = new ArSystem.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .metadata(systemMetadata)
                .localHostnamePort("localhost", localPort)
                .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(srSocketAddress))
                .build();

            HttpService serviceA = new SimpleService().getService("a");
            HttpService serviceB = new SimpleService().getService("b");

            system.provide(serviceA)
                .flatMap(result -> system.provide(serviceB))
                .ifSuccess(result -> System.out.println(identity.commonName() + "Providing example services..."))
                .onFailure(e -> {
                    e.printStackTrace();
                });


        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
