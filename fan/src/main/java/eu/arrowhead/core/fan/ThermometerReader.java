package eu.arrowhead.core.fan;

import se.arkalix.ArSystem;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import eu.arrowhead.core.common.TemperatureDto;

public class ThermometerReader {

    private final HttpClient httpClient;
    private final InetSocketAddress thermometerAddress;

    public ThermometerReader(final HttpClient httpClient, final InetSocketAddress thermometerAddress) {
        this.httpClient = httpClient;
        this.thermometerAddress = thermometerAddress;
    }

    public void start() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                readThermometer(thermometerAddress);
            }
        }, 0, 100);
    }

    private void readThermometer(final InetSocketAddress address) {
        httpClient.send(address, new HttpClientRequest()
            .method(HttpMethod.GET)
            .uri("/temperature/temp")
            .header("accept", "application/json"))
            .flatMap(response -> response.bodyToIfSuccess(TemperatureDto::decodeJson))
            .ifSuccess(result -> {
                System.out.println("Success");
            })
            .onFailure(Throwable::printStackTrace);
    }

}
