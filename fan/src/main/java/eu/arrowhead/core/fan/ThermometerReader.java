package eu.arrowhead.core.fan;

import eu.arrowhead.core.common.TemperatureDto;
import se.arkalix.ArSystem;
import se.arkalix.ServiceRecord;
import se.arkalix.codec.CodecType;
import se.arkalix.net.ProtocolType;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.query.ServiceQuery;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class ThermometerReader {

    final ArSystem system;
    private final HttpClient httpClient;
    private double temperature = 0;

    public ThermometerReader(final ArSystem system, final HttpClient httpClient) {
        this.system = system;
        this.httpClient = httpClient;
    }

    public void start() {

        final ServiceQuery serviceQuery = system.consume()
            .name("temperature")
            .codecTypes(CodecType.JSON)
            .protocolTypes(ProtocolType.HTTP);

        int updateInterval = 1000;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                readThermometer(serviceQuery);
            }
        }, 0, updateInterval);
    }

    private void readThermometer(ServiceQuery serviceQuery) {
        serviceQuery.resolveAll()
            .ifSuccess(services -> {
                if (services.isEmpty()) {
                    System.out.println("No temperature providers detected.");
                    temperature = 0;
                    return;
                }

                Futures.serialize(services.stream().map(this::readThermometer))
                    .ifSuccess(result -> {
                        temperature = result.stream()
                            .map(TemperatureDto::celsius)
                            .mapToDouble(t -> t)
                            .average()
                            .orElse(0);
                        System.out.println("Average temperature: " + temperature);
                    })
                    .onFailure(Throwable::printStackTrace);
            })
            .onFailure(Throwable::printStackTrace);
    }

    private Future<TemperatureDto> readThermometer(final ServiceRecord service) {
        final InetSocketAddress address = service.provider().socketAddress();

        return httpClient
            .send(address, new HttpClientRequest()
                .method(HttpMethod.GET)
                .uri(service.uri() + "/temp")
                .header("accept", "application/json"))
            .flatMap(response -> response.bodyToIfSuccess(TemperatureDto::decodeJson));
    }

    public double getTemperature() {
        return temperature;
    }

}
