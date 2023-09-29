package edu.brown.cs.student.main.handlers;

import com.squareup.moshi.Moshi;
import edu.brown.cs.student.main.parser.MyParser;
import edu.brown.cs.student.main.rowhandler.CreatorFromRow;
import edu.brown.cs.student.main.rowhandler.FactoryFailureException;
import edu.brown.cs.student.main.rowhandler.RowHandler;
import edu.brown.cs.student.main.server.Dataset;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import spark.Request;
import spark.Response;
import spark.Route;


/**
 * The LoadHandler class deals with requests related to loading CSV files. It expects a "filepath" query
 * parameter specifying the path to the CSV file to be loaded, taking in a specified Dataset.
 */
public class LoadHandler implements Route {
  private final Dataset data;
  // create a parser field? feed in the parser here?

  /**
   * Constructs a new LoadHandler instance with the specified Dataset.
   *
   * @param current The dataset to be used for viewing.
   */
  public LoadHandler(Dataset current) {
    this.data = current;
  }

  /**
   * Method that handles an HTTP request to load a dataset from a file. The MyParser class is used to parse
   * the CSV file, and the `CreatorFromRow` interface and custom `Creator` class are used to specify how rows
   * from the CSV file are transformed into lists of strings. Upon successful loading, it updates the dataset in
   * the `Dataset` object, and if an error occurs during loading, it generates a JSON response indicating the failure.
   *
   * @param request  the HTTP request containing the file path to load.
   * @param response the HTTP response to be populated with success or failure messages.
   * @return a success message if the file is loaded successfully; otherwise, a loading failure message in JSON format.
   * @throws Exception if an error occurs during file loading or response construction.
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    // we either do a success response or a fail response
    String path = request.queryParams("filepath");
    try {
      FileReader freader = new FileReader(path);
//      RowHandler creator = new RowHandler();
      class Creator implements CreatorFromRow<List<String>> {
        @Override
        public List<String> create(List<String> row) throws FactoryFailureException {
          return row;
        }
      }

      MyParser<List<String>> parser = new MyParser<>(freader, new Creator());
      parser.toParse();
      this.data.setDataset(parser.getDataset());
      return "File " + path + " loaded successfully!";
    } catch (IOException e) {
      return new LoadingFailureResponse("Error loading file: " + path).serialize();
    }
  }

  /**
   * A record representing a loading failure response.
   * It can be serialized to JSON format.
   */
  public record LoadingFailureResponse(String response_type) {
    /**
     * @return this response, serialized as Json
     */
    String serialize() {
      Moshi moshi = new Moshi.Builder().build();
      return moshi.adapter(LoadingFailureResponse.class).toJson(this);
    }
  }

}
