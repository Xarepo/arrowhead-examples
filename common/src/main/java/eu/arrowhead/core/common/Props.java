package eu.arrowhead.core.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class Props {

    private final static String FILENAME = "application.properties";
    private Properties appProps;

    /**
     * Loads the resource at the given path.
     * {@code path} is first treated as a regular file path. If the resource
     * cannot be found at that location, an attempt is made to load it from
     * resources (i.e. within the jar file).
     *
     * @param path path to the resource.
     * @return An {@code InputStream} object representing the resource.
     */
    private static InputStream loadResource(final String path) throws IOException {
        File file = new File(path);
        if (file.isFile()) {
            return new FileInputStream(file);
        } else {
            return Props.class.getResourceAsStream("/" + path);
        }
    }

    public String getString(final String propName) {
        Objects.requireNonNull(propName, "Expected property name");
        final String result = appProps.getProperty(propName);
        if (result == null) {
            throw new IllegalArgumentException("Missing field '" + propName + "' in application properties.");
        }
        return result;
    }

    public int getInt(final String propName) {
        return Integer.parseInt(getString(propName));
    }

    public Properties load(final String path) throws PropertyException {
        appProps = new Properties();
        File file = new File(path);

        if (file.isFile()) {
            try (InputStream in = loadResource(path)) {
                appProps.load(in);
            } catch (final IOException e) {
                throw new PropertyException("Failed to load " + FILENAME, e);
            }
        } else {
            throw new PropertyException("Could not load application properties file " + path);
        }

        return appProps;
    }

}
