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
import java.io.File;
import java.io.FileInputStream;
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

public class PdeTesterMain {

    private static final Logger logger = LoggerFactory.getLogger(PdeTesterMain.class);
    private final static Properties appProps = new Properties();

    private static KeyStore loadKeyStore(String path, char[] password) {

        KeyStore keyStore = null;

        try (InputStream in = getResource(path)) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(in, password);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            logger.error("Failed to load key store", e);
            System.exit(1);
        }

        return keyStore;
    }

    /**
     * Loads the resource at the given path.
     * {@code path} is first treated as a regular file path. If the resource
     * cannot be found at that location, an attempt is made to load it from
     * resources (i.e. within the jar file).
     *
     * @param path path to the resource.
     * @return An {@code InputStream} object representing the resource.
     */
    private static InputStream getResource(final String path) throws IOException {
        File file = new File(path);
        if (file.isFile()) {
            return new FileInputStream(file);
        } else {
            return PdeTesterMain.class.getResourceAsStream("/" + path);
        }
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


    public static void main(final String[] args) {

        try (InputStream in = getResource(PropNames.FILENAME)) {
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
        final String localAddress = getProp(PropNames.LOCAL_HOSTNAME);
        final int localPort = getIntProp(PropNames.LOCAL_PORT);
        final String srAddress = getProp(PropNames.SR_HOSTNAME);
        final int srPort = getIntProp(PropNames.SR_PORT);

        try {

            TrustStore trustStore = TrustStore.from(loadKeyStore(trustStorePath, trustStorePassword));

            OwnedIdentity identity = new OwnedIdentity.Loader()
                .keyStore(loadKeyStore(keyStorePath, keyStorePassword))
                .keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword)
                .load();

            Arrays.fill(keyPassword, '\0');
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

            final String pdeHostname = getProp(PropNames.PDE_HOSTNAME);
            final int pdePort = Integer.parseInt(getProp(PropNames.PDE_PORT));
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
