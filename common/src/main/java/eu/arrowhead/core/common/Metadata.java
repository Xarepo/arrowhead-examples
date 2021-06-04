package eu.arrowhead.core.common;

import java.util.Map;

public class Metadata {

    private Metadata() {}

    public static Map<String, String> getSystemMetadata(String uniqueIdentifier) {
        return Map.of("system", uniqueIdentifier);
    }

    public static Map<String, String> getServiceMetadata(String uniqueIdentifier) {
        return Map.of("service", uniqueIdentifier);
    }

    public static Map<String, String> getRequesterMetadata(String uniqueIdentifier) {
        return Map.of("port", uniqueIdentifier);
    }
}
