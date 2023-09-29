package edu.brown.cs.student.main.handlers;

import static spark.Spark.connect;

import com.squareup.moshi.Types;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import okio.Buffer;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import spark.Request;
import spark.Response;
import spark.Route;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.net.URLConnection;
import java.util.*;

/**
 * The BroadbandHandler class handles HTTP requests related to broadband data retrieval.
 * It communicates with an external ACS API to fetch data on broadband access for a user-specified
 * state and county. Implements the `Route` Spark interface in order to create a mapping between
 * the HTTP request path.
 */
public class BroadbandHandler implements Route {

//  public class CensusApiResponse {
//    private List<String> headers;
//    private List<StateInfo> states;
//
//    // Getters and setters for headers and states
//
//    public static class StateInfo {
//      private String name;
//      private String code;
//
//      // Getters and setters for name and code
//    }
//  }

  /**
   * Method that handles an HTTP request to fetch broadband data for a specified state and county.
   * This method communicates with an external ACS API to retrieve broadband access statistics, and
   * constructs an HTTP response containing the retrieved data or appropriate error messages.
   *
   * @param request  the HTTP request containing query parameters for state and county.
   * @param response the HTTP response to be populated with broadband data or error messages.
   * @return null, as the data or error messages are added to the HTTP response.
   * @throws Exception if an error occurs during data retrieval or processing.
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    // get date and time
    String timestamp = getDateTime();
    // request parameters
    String stateName = request.queryParams("state");
    String countyName = request.queryParams("county");
    // retrieve code for state and county
    String stateCode = getStateCode(stateName);
    String countyCode = getCountyCode(stateCode, countyName);

    System.out.println(timestamp);

    // request data for given state and county
    if (stateCode != null && countyCode != null) {
      System.out.println("e");

      try {
        String apiKey = "api_key";
        String apiUrl = "https://api.census.gov/data/2021/acs/acs1/subject/variables?get=NAME,S2802_C03_001E&for=county:" + countyCode
            + "*&in=state:" + stateCode + "&key=" + apiKey;

        URL url = new URL(apiUrl);

        HttpURLConnection requestURL = (HttpURLConnection) url.openConnection();

        int responseCode = requestURL.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
          Moshi moshi = new Moshi.Builder().build();
          JsonAdapter<Map<String, List<String>>> adapter = moshi.adapter(Types.newParameterizedType(Map.class, String.class, List.class));

          // parse json into map
          Map<String, List<String>> jsonResponse = adapter.fromJson(new Buffer().readFrom(requestURL.getInputStream()));

          // get data from json
          List<String> broadbandData = jsonResponse.get("S2802_C03_001E");

          // response map w timestamp and data
          Map<String, Object> responseMap = new HashMap<>();
          responseMap.put("timestamp", timestamp);
          responseMap.put("data", broadbandData);
          System.out.println(timestamp + broadbandData);

//          Moshi moshi = new Moshi.Builder().build();
//          JsonAdapter<GridResponse> adapter = moshi.adapter(GridResponse.class).nonNull();
//          // NOTE: important! pattern for handling the input stream
//          GridResponse body = adapter.fromJson(new Buffer().readFrom(requestURL.getInputStream()));
//          requestURL.disconnect();
//          if (body == null || body.properties() == null || body.properties().gridId() == null) {
//            throw new DatasourceException("Malformed response from Census API");
//          }
//
//          // return the extracted census data
//          Map<String, Object> responseMap = new HashMap<>();
//          responseMap.put("timestamp", timestamp);
//          responseMap.put("data", broadbandData);
//
//          return body;
        } else {
          // handle failed API request
          throw new DatasourceException("API request failed with response code: " + responseCode);
        }

      } catch (IOException e) {
        throw new DatasourceException(e.getMessage());
      }
    }
    return null;
  }

  /**
   * Helper method that retrieves the current date and time of the request in a specific format.
   *
   * @return a formatted string representing the current date and time.
   */
  private String getDateTime() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date now = new Date();
    return dateFormat.format(now);
  }

  /**
   * Helper method that makes an API request to retrieve the state code based on the provided state name.
   * Returns null if state name not found.
   *
   * @param stateName the name of the state for which to retrieve the code.
   * @return the state code corresponding to the provided state name.
   * @throws IOException if an error occurs during the API request.
   */
  private String getStateCode(String stateName) throws IOException {
    // make an API request to get the state code based on the state name provided
    String apiUrl = "https://api.census.gov/data/2010/dec/sf1?get=NAME&for=state:*";
    HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
    try {
      connection.setRequestMethod("GET");
      connection.connect();

      int responseCode = connection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<List<List<String>>> adapter = moshi.adapter(Types.newParameterizedType(List.class, List.class));

        // parse the json response into a List
        List<List<String>> jsonResponse = adapter.fromJson(new Buffer().readFrom(connection.getInputStream()));

        // iterate through the list to find the state code for the target state name
        for (List<String> row : jsonResponse) {
          if (row.size() >= 2) {
            String name = row.get(0);
            String code = row.get(1);

            if (stateName.equalsIgnoreCase(name)) {
              System.out.println(stateName + ": " + code);
              return code;
            }
          }
        }
      }
    } finally {
      connection.disconnect();
    }
    return null;
  }

  /**
   * Helper method that makes an API request to retrieve the county code based on the provided
   * state code and county name. Returns null if county name not found.
   *
   * @param stateCode  the state code for the target state.
   * @param countyName the name of the county for which to retrieve the code.
   * @return the county code corresponding to the provided state code and county name.
   * @throws IOException if an error occurs during the API request.
   */
  private String getCountyCode(String stateCode, String countyName) throws IOException {
    // api request to get county code based on county name and state code
    if (stateCode != null) {
      String apiUrl = "https://api.census.gov/data/2010/dec/sf1?get=NAME&for=county:*&in=state:" + stateCode;

      HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();

      try {
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
          Moshi moshi = new Moshi.Builder().build();
          JsonAdapter<List<List<String>>> adapter = moshi.adapter(Types.newParameterizedType(List.class, List.class));

          // Parse the JSON response into a List
          List<List<String>> jsonResponse = adapter.fromJson(new Buffer().readFrom(connection.getInputStream()));

          // Iterate through the list to find the county code for the target county name
          for (List<String> row : jsonResponse) {
            if (row.size() >= 3) {
              String fullName = row.get(0);
//              String sCode = row.get(1);
              String countyCode = row.get(2);
              // Split the full name to extract the county name
              String[] parts = fullName.split(",");
              if (parts.length >= 2) {
                String cName = parts[0].trim(); // Extracted county name

                // Check if the county name and state match the user's request
                if (countyName.equalsIgnoreCase(cName)) {
                  System.out.println(countyName + ": " + countyCode);

                  return countyCode;
                }
              }
            }

          }
        }
      } finally {
        connection.disconnect();
      }
    }

    // Return null if county name not found
    return null;
  }

  public record GridResponse(String id, GridResponseProperties properties) { }
  // Note: case matters! "gridID" will get populated with null, because "gridID" != "gridId"
  public record GridResponseProperties(String gridId) {}

}
