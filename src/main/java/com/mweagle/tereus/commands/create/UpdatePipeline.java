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
package com.mweagle.tereus.commands.create;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.google.gson.Gson;
import com.mweagle.tereus.NashornEvaluator;

public class UpdatePipeline extends NashornEvaluator
{
    private static final String BINDING_PACKAGE = "com.mweagle.tereus.commands.evaluation";
    private static final String[] BINDING_CLASSES = {"common.FileUtils",
    												"update.JSONPatchUtils"};  
    
    private static final String BINDING_RESOURCE_ROOT = "bindings";
    private static final String[] JS_FILES = {
    		"node_modules/underscore/underscore-min.js",
            "node_modules/immutable/dist/immutable.min.js",
    		"common/init.js",
    		"update/index.js",
    		"node_modules/json8-patch/JSON8Patch.js"
            };
    
	private final Path patchSource;
	private final Map<String, Object> arguments;
	private final String stackName;
	private final AWSCredentials awsCredentials;
	private final boolean dryRun;
	private final Logger logger;
    
	public UpdatePipeline(final Path patchSource, 
							final Map<String, Object> arguments, 
							final String stackName,
							final AWSCredentials awsCredentials, 
							final boolean dryRun, 
							final Logger logger)
    {
		this.patchSource = patchSource;
    	this.arguments = arguments;
    	this.stackName = stackName;
    	this.awsCredentials = awsCredentials;
    	this.dryRun = dryRun;
    	this.logger = logger;
    }

	protected void publishGlobals(ScriptEngine engine)
	{
		// Get the current stack definition
		if (!this.stackName.isEmpty())
		{
			final GetTemplateRequest templateRequest = new GetTemplateRequest().withStackName(this.stackName);
	        final AmazonCloudFormationAsyncClient awsClient = new AmazonCloudFormationAsyncClient(this.awsCredentials);
			final GetTemplateResult templateResult = awsClient.getTemplate(templateRequest);
	        engine.put("TemplateInfoImpl", templateResult);			
		}
		else
		{
			this.logger.warn("StackName not provided. Applied patch result will not be available");
		}
	
		// Stuff the arguments in there...
		Supplier<String> fnArgs = () -> {
            HashMap<String, Map<String, Object>> args = new HashMap<>();
            args.put("arguments", this.arguments);
            Gson gson = new Gson();
            return gson.toJson(args);
        };
        engine.put("ArgumentsImpl", fnArgs);
    	
    	// get the info
    	final AmazonIdentityManagementClient client = new AmazonIdentityManagementClient();	
    	final GetUserResult result = client.getUser();
        engine.put("UserInfoImpl", result);

        // And the logger
        final Logger templateLogger  = LogManager.getLogger(UpdatePipeline.class);
        engine.put("logger", templateLogger);	
	}
	
	protected Stream<String>  javaClassnames()
	{
		return Arrays.stream(BINDING_CLASSES).map(name -> String.join(".", UpdatePipeline.BINDING_PACKAGE, name));
	}
	
	protected Stream<String> javascriptResources()
	{
		return Arrays.stream(JS_FILES).map(name -> String.join(File.separator, UpdatePipeline.BINDING_RESOURCE_ROOT, name));
	}

	@Override
	public Path getEvaluationSource()
	{
		return this.patchSource;
	}

	@Override
	public boolean isDryRun()
	{
		return this.dryRun;
	}

	@Override
	public Logger getLogger()
	{
		return this.logger;
	}
}
