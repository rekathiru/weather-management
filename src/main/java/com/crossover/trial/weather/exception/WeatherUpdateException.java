package com.crossover.trial.weather.exception;

/**
 * An internal exception wether update issue
 */
public class WeatherUpdateException extends Exception {
    private String message;

    public WeatherUpdateException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
