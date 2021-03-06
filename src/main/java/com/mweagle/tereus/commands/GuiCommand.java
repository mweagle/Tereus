// Copyright (c) 2015 Matt Weagle (mweagle@gmail.com)

// Permission is hereby granted, free of charge, to
// any person obtaining a copy of this software and
// associated documentation files (the "Software"),
// to deal in the Software without restriction,
// including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so,
// subject to the following conditions:

// The above copyright notice and this permission
// notice shall be included in all copies or substantial
// portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
// ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
// SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
package com.mweagle.tereus.commands;

import static spark.Spark.halt;
import static spark.Spark.post;
import static spark.SparkBase.port;
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
import com.mweagle.Tereus;
import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.input.TereusInput;
import com.mweagle.tereus.input.UpdateInput;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "gui", description = "expose a localhost HTTP UI for Stack evaluation")
public class GuiCommand extends AbstractTereusCommand {

    @Option(name = {"-p", "--port"}, description = "Alternative port for UI HTTP server")
    public int port = 4567;
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        staticFileLocation("/web");
        port(this.port);
        /**
         * The creation endpoint
         */
        post("/api/create", (request, response) -> {
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
                    TereusInput tereusInput =  new TereusInput(path,
                                                                region,
                                                                parameters,
                                                                tags,
                                                                true);

                    final ByteArrayOutputStream osStream = new ByteArrayOutputStream();
                    new CreateCommand().create(tereusInput, Optional.of(osStream));

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
        
        
        post("/api/update", (request, response) -> {
            final Optional<String> requestInput = Optional.ofNullable(request.body());
            if (requestInput.isPresent() && !requestInput.get().isEmpty())
            {
                final Map<String, Object> jsonRequest = (Map<String, Object>)(new Gson().fromJson(requestInput.get(), Map.class));
                try
                {
                    final String path = (String)jsonRequest.get("path");
                    final String stackName = (String)jsonRequest.get("stackName");
                    final String region = (String)jsonRequest.get("region");
                    final Map<String, Object> arguments = (Map<String, Object>)(jsonRequest.getOrDefault("arguments", new HashMap<>()));

                    final UpdateInput input = new UpdateInput(path, arguments, region, true);
                    final ByteArrayOutputStream osStream = new ByteArrayOutputStream();
                    final Map<String, Object> updateResults = new UpdateCommand().update(input, Optional.of(osStream));

                    final String templateContent = new String(Files.readAllBytes(Paths.get(path)),"UTF-8");

                    HashMap<String, String> results = new HashMap<>();
                    results.put("template", templateContent);
                    results.put("evaluated", osStream.toString("UTF-8"));
                    results.put("target", updateResults.getOrDefault("Target", "").toString());
                    results.put("applied", updateResults.getOrDefault("Result", "").toString());
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
        logger.info(String.format("Tereus UI available at http://localhost:%d/", this.port));		
    }
}
