package com.jethrolai.api.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;

/**
 * 
 * This is an implementation of AWS Lambda function that serves as a facade api
 * between AWS ElasticSearch service's restful api and AWS Api Gateway.
 * 
 * @author jlai
 *
 */
public class ElasticSearchAWSStreamLambdaFacade implements RequestStreamHandler {
	/**
	 * This must be set through AWS Lambda
	 */
	final static private String ES_HOST_ENV_VAR_KEY = "ES_HOST";
	final static private String QUERY_PARAMETERS = "queryStringParameters";
	final static private String DEFAULT_INDEX = "index";
	final static private String DEFAULT_TYPE = "type";

	private JSONParser parser = new JSONParser();

	/**
	 * entry point of lambda function. This assumes API Gateway uses Lambda
	 * Proxy Integration where all external requests are proxied to lambda with
	 * details in the "event" object of input context.
	 */

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		// TODO validation: check if required environmental variables are
		// provided; if not, log the error, issue
		// alert/notification and gracefully quit.
		LambdaLogger logger = this.setUpLogger(context);
		logger.log("Initializing ElasticSearch Lambda Facade ...");

		logger.log("Checking required environment setup ...");
		String esHost = System.getenv(ES_HOST_ENV_VAR_KEY);
		if (esHost == null)
			throw new RuntimeException(
					String.format("Environmental variable \"%s\" must be provided.", ES_HOST_ENV_VAR_KEY));
		else if (esHost.startsWith("http://") == false && esHost.startsWith("https://") == false)
			esHost = "http://" + esHost;

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		JSONObject responseJson = new JSONObject();
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		logger.log("Function initialized.");

		// Parse incoming request and set up search term
		try {
			JSONObject event = (JSONObject) parser.parse(reader);
			logger.log("Event:\n\n");
			logger.log(((JSONObject) event).toString());
			if (event.get(QUERY_PARAMETERS) != null) {
				JSONObject queryParameters = (JSONObject) event.get(QUERY_PARAMETERS);
				logger.log(String.format("Setting query terms ..."));
				queryParameters.forEach((k, v) -> {
					queryBuilder.must(QueryBuilders.matchQuery((String) k, v));
					logger.log(String.format("  query term: \"%s:%s\" added.", k, v));
				});
				logger.log(String.format("Setting query terms completed!"));
			} else {
				logger.log("No query parameter provided.");
			}

			logger.log(String.format("Search will be performed on index:%s and type:%s", this.getIndex(event),
					this.getType(event)));
			Search search = (Search) new Search.Builder(String.format("{\"query\":%s}", queryBuilder.toString()))
					// TODO multiple indexs and/or types can be supported
					.addIndex(this.getIndex(event)).addType(this.getType(event)).build();

			// Initialize JsetClient as search utility
			JestClientFactory factory = new JestClientFactory();
			HttpClientConfig clientConfig = new HttpClientConfig.Builder(esHost).multiThreaded(true)
					.defaultMaxTotalConnectionPerRoute(10).maxTotalConnection(20).build();
			factory.setHttpClientConfig(clientConfig);
			JestClient client = factory.getObject();

			logger.log("Performing searching ...");
			JestResult result = client.execute(search);
			logger.log("Completed.");

			logger.log("Constructing response ...");
			JSONObject headerJson = new JSONObject();
			headerJson.put("Content-Type", "application/json");
			responseJson.put("statusCode", "200");
			responseJson.put("headers", headerJson);
			responseJson.put("body", result.getJsonString());
			responseJson.put("isBase64Encoded", "true");
			logger.log("Completed.");
		} catch (ParseException pex) {
			logger.log("Failed to parse incoming request ...");
			responseJson.put("statusCode", "400");
			responseJson.put("exception", pex);
			logger.log(pex.getMessage());
		}

		logger.log("Writing response to output stream ...");
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		writer.write(responseJson.toJSONString());
		writer.close();
		logger.log("Completed.");
	}

	/**
	 * Extra encapsulation for testability
	 * 
	 * @param context
	 * @return
	 */
	protected LambdaLogger setUpLogger(Context context) {
		return context.getLogger();
	}

	/**
	 * TODO implement query for specified index
	 * 
	 * @param event
	 *            parsed request as JSONOject
	 */
	private String getIndex(JSONObject event) {
		return DEFAULT_INDEX;
	}

	/**
	 * TODO implement query for specified type
	 * 
	 * @param event
	 *            parsed request as JSONOject
	 */
	private String getType(JSONObject event) {
		return DEFAULT_TYPE;
	}
}