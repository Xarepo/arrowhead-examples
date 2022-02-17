package eu.arrowhead.core.fan;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import se.arkalix.ArSystem;
import se.arkalix.ServiceInterface;
import se.arkalix.ServiceRecord;
import se.arkalix.codec.CodecType;
import se.arkalix.net.ProtocolType;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.query.ServiceQuery;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

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
            .name("thermometer")
            .codecTypes(CodecType.JSON)
            .protocolTypes(ProtocolType.HTTP);

        int updateInterval = 4000;
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
										System.out.println("No services found");
                    temperature = 0;
                    return;
                }

                Futures.serialize(services.stream().map(this::readThermometer))
                    .ifSuccess(result -> {
                        temperature = result.stream()
                            .mapToDouble(t -> t)
                            .average()
                            .orElse(0);
                        System.out.println("Average temperature: " + temperature);
                    })
                    .onFailure(Throwable::printStackTrace);
            })
            .onFailure(Throwable::printStackTrace);
    }

		private Future<Double> readThermometer(final ServiceRecord service) {
			final InetSocketAddress address = service.provider().socketAddress();
			var token = service.interfaceTokens().get(ServiceInterface.HTTP_SECURE_JSON);

			if (token == null) {
				throw new RuntimeException("No token!");
			}

			final var request = new HttpClientRequest()
					.method(HttpMethod.GET)
					.uri(service.uri() + "/properties/temperature?token=" + token)
					// .uri("/toberemoved?token=" + token)
					.header("accept", "application/json");

					return httpClient
					.send(address, request)
					.map(response -> {
						System.out.println("response.status():");
						System.out.println(response.status());
						return response;
					})
					.flatMap(response -> response.bodyAsString())
					.map(Double::parseDouble);
		}

    public double getTemperature() {
        return temperature;
    }

}
