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
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import com.google.gson.Gson;
import com.mweagle.tereus.input.TereusInput;

public class CreationPipeline extends AWSEvaluationPipeline
{
    private static final String BINDING_PACKAGE = "com.mweagle.tereus.commands.evaluation";
    private static final String[] BINDING_CLASSES = {"common.FileUtils",
    												"common.LambdaUtils",	
    												"create.CloudFormationTemplateUtils",
                                                    };  

    private static final String BINDING_RESOURCE_ROOT = "bindings";
    private static final String[] JS_FILES = {
    		"node_modules/underscore/underscore-min.js",
            "node_modules/immutable/dist/immutable.min.js",
    		"common/init.js",
    		"create/index.js",
            "create/CONSTANTS.js",
            "create/CloudFormationTemplate.js",
            /** AWS Helpers **/
            "create/aws/index.js",
            "create/aws/lambda.js",
            "create/aws/ec2.js",
            "create/aws/iam.js"
            };
    
    private final TereusInput cfInput; 
 
	public CreationPipeline(final TereusInput input)
    {
		super(input.awsCredentials, input.awsRegion, input.logger);

    	this.cfInput = input;
    }
 
	protected void publishGlobals(ScriptEngine engine)
	{
		super.publishGlobals(engine);
		
		Supplier<String> fnArgs = () -> {
            HashMap<String, Map<String, Object>> args = new HashMap<>();
            args.put("params", this.cfInput.params);
            args.put("tags", this.cfInput.tags);
            Gson gson = new Gson();
            return gson.toJson(args);
        };
        engine.put("ArgumentsImpl", fnArgs);
	}
	
	protected Stream<String>  javaClassnames()
	{
		return Arrays.stream(BINDING_CLASSES).map(name -> String.join(".", CreationPipeline.BINDING_PACKAGE, name));
	}
	
	protected Stream<String> javascriptResources()
	{
		return Arrays.stream(JS_FILES).map(name -> String.join(File.separator, CreationPipeline.BINDING_RESOURCE_ROOT, name));
	}

	@Override
	public Path getEvaluationSource()
	{
		return this.cfInput.stackDefinitionPath;
	}

	@Override
	public boolean isDryRun()
	{
		return this.cfInput.dryRun;
	}
}
