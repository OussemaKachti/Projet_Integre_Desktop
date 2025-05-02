package com.esprit.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Random;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Service to retrieve weather forecast data
 */
public class ServiceWeather {
    // Configured OpenWeatherMap API key
    private static final String API_KEY = "c972b1f80aeb7d0fb525e4e00b5c6f38";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/forecast";

    // Cache for storing generated weather data per location
    private static final java.util.Map<String, JSONObject> locationWeatherCache = new java.util.HashMap<>();

    /**
     * Retrieves the weather forecast for a given location and date
     * @param location City name or coordinates
     * @param eventDate Date of the event
     * @return JSONObject containing the weather data
     */
    public JSONObject getWeatherForecast(String location, Date eventDate) {
        try {
            // Real API version (use with actual API key)
            if (!API_KEY.equals("YOUR_API_KEY")) {
                String urlString = API_URL + "?q=" + location + "&appid=" + API_KEY + "&units=metric";
                URL url = new URL(urlString);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONParser parser = new JSONParser();
                    JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());

                    return findForecastForDate(jsonResponse, eventDate);
                } else {
                    System.err.println("Weather API request error: " + responseCode);
                    return getDummyWeatherData(location);
                }
            } else {
                return getDummyWeatherData(location);
            }
        } catch (IOException | ParseException e) {
            System.err.println("Error retrieving weather data: " + e.getMessage());
            e.printStackTrace();
            return getDummyWeatherData(location);
        }
    }

    /**
     * Generates simulated weather data when no API key is available
     * @param location City for which to generate weather data
     * @return JSONObject with mock weather data
     */
    private JSONObject getDummyWeatherData(String location) {
        try {
            if (locationWeatherCache.containsKey(location)) {
                return locationWeatherCache.get(location);
            }

            JSONObject weatherData = new JSONObject();

            Random random = new Random(location.hashCode());
            double temperature = 15.0 + random.nextDouble() * 20.0;
            temperature = Math.round(temperature * 10.0) / 10.0;

            JSONObject main = new JSONObject();
            main.put("temp", temperature);
            main.put("feels_like", temperature + (random.nextDouble() * 2.0 - 1.0));
            main.put("humidity", 40 + random.nextInt(50));
            weatherData.put("main", main);

            String description;
            String icon;
            if (temperature < 18.0) {
                description = "Cloudy";
                icon = "04d";
            } else if (temperature < 22.0) {
                description = "Partly cloudy";
                icon = "02d";
            } else if (temperature < 28.0) {
                description = "Sunny";
                icon = "05d";
            } else {
                description = "Very hot";
                icon = "03d";
            }

            JSONObject weatherDetails = new JSONObject();
            weatherDetails.put("description", description);
            weatherDetails.put("icon", icon);

            JSONArray weatherArray = new JSONArray();
            weatherArray.add(weatherDetails);
            weatherData.put("weather", weatherArray);

            weatherData.put("dt", System.currentTimeMillis() / 1000);

            JSONObject wind = new JSONObject();
            wind.put("speed", 1.0 + random.nextDouble() * 8.0);
            weatherData.put("wind", wind);

            locationWeatherCache.put(location, weatherData);

            return weatherData;
        } catch (Exception e) {
            System.err.println("Error generating dummy weather data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Finds the forecast closest to the specified event date
     * @param jsonResponse Full JSON response from the API
     * @param eventDate Date of the event
     * @return JSONObject containing the matching forecast
     */
    private JSONObject findForecastForDate(JSONObject jsonResponse, Date eventDate) {
        try {
            JSONArray forecasts = (JSONArray) jsonResponse.get("list");

            long eventTime = eventDate.getTime() / 1000;
            JSONObject closestForecast = null;
            long smallestDifference = Long.MAX_VALUE;

            for (Object forecastObj : forecasts) {
                JSONObject forecast = (JSONObject) forecastObj;
                long forecastTime = (long) forecast.get("dt");
                long difference = Math.abs(forecastTime - eventTime);

                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    closestForecast = forecast;
                }
            }

            return closestForecast;
        } catch (Exception e) {
            System.err.println("Error parsing forecast data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extracts temperature from the weather data
     * @param weatherData JSON weather object
     * @return Temperature in Celsius
     */
    public double getTemperature(JSONObject weatherData) {
        if (weatherData == null) return Double.NaN;

        try {
            JSONObject main = (JSONObject) weatherData.get("main");
            return (double) main.get("temp");
        } catch (Exception e) {
            System.err.println("Error extracting temperature: " + e.getMessage());
            return Double.NaN;
        }
    }

    /**
     * Extracts weather description from the data
     * @param weatherData JSON weather object
     * @return Weather description (e.g., "Sunny", "Rainy")
     */
    public String getWeatherDescription(JSONObject weatherData) {
        if (weatherData == null) return "Unavailable";

        try {
            JSONArray weatherArray = (JSONArray) weatherData.get("weather");
            JSONObject weather = (JSONObject) weatherArray.get(0);
            return (String) weather.get("description");
        } catch (Exception e) {
            System.err.println("Error extracting weather description: " + e.getMessage());
            return "Unavailable";
        }
    }

    /**
     * Extracts weather icon code from the data
     * @param weatherData JSON weather object
     * @return Weather icon code
     */
    public String getWeatherIcon(JSONObject weatherData) {
        if (weatherData == null) return "01d"; // Default icon (clear sky)

        try {
            JSONArray weatherArray = (JSONArray) weatherData.get("weather");
            JSONObject weather = (JSONObject) weatherArray.get(0);
            return (String) weather.get("icon");
        } catch (Exception e) {
            System.err.println("Error extracting weather icon: " + e.getMessage());
            return "01d";
        }
    }
}
