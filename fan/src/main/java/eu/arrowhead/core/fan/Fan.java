package eu.arrowhead.core.fan;

import se.arkalix.net.http.client.HttpClient;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;

public class Fan {

    public static void main(final String[] args) {
        if (args.length != 3) {
            System.out.println(args);


            System.exit(1);
        }

        final Path keyStorePath = Path.of(args[0]);
        final Path trustStorePath = Path.of(args[1]);
        final int thermometerPort = Integer.parseInt(args[2]);

        try {
            // Load owned system identity and truststore.
            final char[] password = new char[] {'1', '2', '3', '4', '5', '6'};
            final OwnedIdentity identity = new OwnedIdentity.Loader()
                .keyPassword(password)
                .keyStorePath(keyStorePath)
                .keyStorePassword(password)
                .load();
            final TrustStore trustStore = TrustStore.read(trustStorePath, password);
            Arrays.fill(password, '\0');

            final HttpClient httpClient = new HttpClient.Builder()
                .identity(identity)
                .trustStore(trustStore)
                .build();

            final var thermometerAddress = new InetSocketAddress("localhost", thermometerPort);
            final ThermometerReader thermometerReader = new ThermometerReader(httpClient, thermometerAddress);
            thermometerReader.start();

        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
