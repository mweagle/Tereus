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
package com.mweagle.tereus.commands.pipelines;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.google.gson.Gson;
import com.amazonaws.services.cloudformation.model.Tag;

public class UpdatePipeline extends AWSEvaluationPipeline
{
    private static final String BINDING_PACKAGE = "com.mweagle.tereus.commands.evaluation";
    private static final String[] BINDING_CLASSES = {"common.FileUtils",
    												"update.JSONPatchUtils"};  
    
    private static final String BINDING_RESOURCE_ROOT = "bindings";
    private static final String[] JS_FILES = {
    		"node_modules/underscore/underscore-min.js",
            "node_modules/immutable/dist/immutable.min.js",
    		"common/init.js",
    		"update/CONSTANTS.js",
    		"update/index.js",
    		"node_modules/json-patch/jsonpatch.js"
            };
    
	private final Path patchSource;
	private final Map<String, Object> arguments;
	private final GetTemplateResult stackTemplateResult;
	private final Stack 	stackInfo;
	private final boolean dryRun;
    
	public UpdatePipeline(final Path patchSource, 
							final Map<String, Object> arguments, 
							final GetTemplateResult stackTemplateResult,
							final Stack stackInfo,
							final AWSCredentials awsCredentials, 
							final Region awsRegion,
							final boolean dryRun, 
							final Logger logger)
    {
		super(awsCredentials, awsRegion, logger);
		
		this.patchSource = patchSource;
    	this.arguments = arguments;
    	this.stackTemplateResult = stackTemplateResult;
    	this.stackInfo = stackInfo;
    	this.dryRun = dryRun;
    }

	protected void publishGlobals(ScriptEngine engine)
	{
		super.publishGlobals(engine);
		
		// Get the current stack definition
		if (null != this.stackTemplateResult)
		{
	        engine.put("TemplateInfoImpl", this.stackTemplateResult);			
		}
		else
		{
			getLogger().warn("StackName not provided. Applied patch result will not be available");
		}
	
        final Map<String, Object> tags = stackInfo.
        									getTags().
        									stream().
        									collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        
		Supplier<String> fnArgs = () -> {
            HashMap<String, Map<String, Object>> args = new HashMap<>();
            args.put("arguments", this.arguments);
            args.put("tags", tags);
            Gson gson = new Gson();
            return gson.toJson(args);
        };
        engine.put("ArgumentsImpl", fnArgs);
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
}
