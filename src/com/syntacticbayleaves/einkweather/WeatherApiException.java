package com.syntacticbayleaves.einkweather;

public class WeatherApiException extends Exception {
    private static final long serialVersionUID = 1L;

    public WeatherApiException() {
        super();
    }

    public WeatherApiException(String detailMessage) {
        super(detailMessage);
    }

    public WeatherApiException(Throwable throwable) {
        super(throwable);
    }

    public WeatherApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
