package eu.arrowhead.core.pdetester;

import se.arkalix.ArServiceRecordCache;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.or.OrchestrationOption;
import se.arkalix.core.plugin.or.OrchestrationPattern;
import se.arkalix.core.plugin.or.OrchestrationStrategy;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdeTester {
    
    private static final Logger logger = LoggerFactory.getLogger(PdeTester.class);
    private final static Properties appProps = new Properties();

    private static KeyStore loadKeyStore(String path, char[] password) {

        KeyStore keyStore = null;

        try (InputStream in = PdeTester.class.getResourceAsStream("/" + path)) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(in, password);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            logger.error("Failed to load key store", e);
            System.exit(1);
        }

        return keyStore;
    }

    private static String getProp(final String propName) {
        final String result = appProps.getProperty(propName);
        if (result == null) {
            throw new IllegalArgumentException("Missing field '" + propName + "' in application properties.");
        }
        return result;
    }

    private static int getIntProp(final String propName) {
        return Integer.parseInt(getProp(propName));
    }

    public static void main(final String[] args) {

        try (InputStream in = PdeTester.class.getResourceAsStream("/" + PropNames.FILENAME)) {
            appProps.load(in);
        } catch (final IOException e) {
            logger.error("Failed reading " + PropNames.FILENAME, e);
            System.exit(1);
        }

        final char[] keyPassword = getProp(PropNames.KEY_PASSWORD).toCharArray();
        final String keyStorePath = getProp(PropNames.KEY_STORE);
        final char[] keyStorePassword = getProp(PropNames.KEY_STORE_PASSWORD).toCharArray();
        final String trustStorePath = getProp(PropNames.TRUST_STORE);
        final char[] trustStorePassword = getProp(PropNames.TRUST_STORE_PASSWORD).toCharArray();
        final String localAddress = getProp(PropNames.LOCAL_ADDRESS);
        final int localPort = getIntProp(PropNames.LOCAL_PORT);
        final String srAddress = getProp(PropNames.SR_ADDRESS);
        final int srPort = getIntProp(PropNames.SR_PORT);


        try {

            TrustStore trustStore = TrustStore.from(loadKeyStore(trustStorePath, trustStorePassword));
            OwnedIdentity identity = new OwnedIdentity.Loader()
                .keyStore(loadKeyStore(keyStorePath, keyStorePassword))
                .keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword)
                .load();

            // final var trustStore = TrustStore.read(trustStorePath, trustStorePassword);
            Arrays.fill(keyPassword, '\0');
            Arrays.fill(trustStorePassword, '\0');

            final var srSocketAddress = new InetSocketAddress(srAddress, srPort);

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

            final ThermometerReader thermometerReader1 = new ThermometerReader(system, httpClient);
            final ThermometerReader thermometerReader2 = new ThermometerReader(system, httpClient);

            thermometerReader1.start();
            thermometerReader2.start();

            final PdeTest test = new PdeTest(system, httpClient, thermometerReader1, thermometerReader2);
            test.start()
                .ifSuccess(result -> {
                    logger.info("The PDE is running correctly.");
                    System.exit(0);
                })
                .onFailure(e -> {
                    fail(e);
                });

        } catch (final Throwable e) {
            fail(e);
        }
    }

    private static void fail(Throwable e) {
        logger.error("Test failed", e);
        System.exit(1);
    }
}
