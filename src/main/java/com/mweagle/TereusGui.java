package com.mweagle;

import static spark.Spark.halt;
import static spark.Spark.post;
import static spark.SparkBase.staticFileLocation;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.mweagle.tereus.CONSTANTS;
public class TereusGui {
		
	 @SuppressWarnings("unchecked")
	public static void main(String[] args) {
	        staticFileLocation("/web");
	        
	        /** 
	         * The single evaluation endpoint
	         */
	        post("/api/evaluator", (request, response) -> {	        	
	        	final Optional<String> requestInput = Optional.ofNullable(request.body());
	        	if (requestInput.isPresent() && !requestInput.get().isEmpty())
	        	{
		        	final Map<String, Object> jsonRequest = (Map<String, Object>)(new Gson().fromJson(requestInput.get(), Map.class));
		        	try
		        	{
			        	final String path = (String)jsonRequest.get("path");
			        	final String stackName = (String)jsonRequest.get("stackName");
			        	final String region = (String)jsonRequest.get("region");
			        	final Map<String, Object> paramsAndArgs = (Map<String, Object>)(jsonRequest.getOrDefault("paramsAndTags", new HashMap<>()));
			            final Map<String, Object> parameters = (Map<String, Object>)paramsAndArgs.getOrDefault(CONSTANTS.ARGUMENT_JSON_KEYNAMES.PARAMETERS, new HashMap<>());
			            final Map<String, Object> tags = (Map<String, Object>)paramsAndArgs.getOrDefault(CONSTANTS.ARGUMENT_JSON_KEYNAMES.TAGS, new HashMap<>());
			            
			            // Create a buffered logger...
			            TereusInput tereusInput =  new TereusInput(stackName,
												            		path,
										                            null,
										                            region,
										                            parameters,
										                            tags,
										                            true);			            
			            
			            final ByteArrayOutputStream osStream = new ByteArrayOutputStream();
		                new Tereus().run(tereusInput, Optional.of(osStream));
		                
		                // Return both the raw template and the evaluated version
		                final String templateContent = new String(Files.readAllBytes(Paths.get(path)),"UTF-8");
		                
		                HashMap<String, String> results = new HashMap<>();
		                results.put("template", templateContent);
		                results.put("evaluated", osStream.toString("UTF-8"));
		                Gson gson = new Gson();
		    			return gson.toJson(results);
		        	}
		        	catch (Exception ex)
		        	{
		        		halt(400, "Invalid request. (Error: " + ex.toString() + ")");
		        	}
	        	}
	        	else
	        	{
	        		halt(400, "Invalid request.  Please provide a JSON object including path, stackName, and arguments.");
	        	}	
	        	return null;
	        });
	        final Logger logger = LogManager.getLogger(Tereus.class.getName());
	        logger.info("Tereus UI available at http://localhost:4567/");
	    }
}
