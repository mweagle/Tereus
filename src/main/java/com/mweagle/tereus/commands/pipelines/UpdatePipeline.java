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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.*;
import com.mweagle.tereus.INashornEvaluationAccumulator;
import com.mweagle.tereus.commands.evaluation.common.FileUtils;
import com.mweagle.tereus.commands.evaluation.common.LambdaUtils;
import com.mweagle.tereus.commands.evaluation.update.CloudFormationPatchUtils;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.google.gson.Gson;

public class UpdatePipeline extends AWSEvaluationPipeline
{
    private static final String BINDING_PACKAGE = "com.mweagle.tereus.commands.evaluation";
    private static final String BINDING_RESOURCE_ROOT = "bindings";
    private static final String[] JS_FILES = {
            "node_modules/underscore/underscore-min.js",
            "node_modules/immutable/dist/immutable.min.js",
            "common/CONSTANTS.js",
            "common/init.js",
            "node_modules/fast-json-patch/dist/json-patch-duplex.min.js",
            "update/CONSTANTS.js",
            "update/index.js"
            };
    
    private final Path patchSource;
    private final Map<String, Object> arguments;
    private final boolean dryRun;
    
    public UpdatePipeline(final Path patchSource,
                            final Map<String, Object> arguments,
                            final AWSCredentials awsCredentials,
                            final Region awsRegion,
                            final boolean dryRun,
                            final Logger logger)
    {
        super(awsCredentials, awsRegion, logger);

        this.patchSource = patchSource.toAbsolutePath();
        this.arguments = arguments;
        this.dryRun = dryRun;
    }

    protected void publishGlobals(ScriptEngine engine)
    {
        super.publishGlobals(engine);

        // Publish a function that accepts a stack target name, defined by the patch
        // file, that represents the target.  We'll push all this
        Function<String, String> fnStackInfo = (stackName) -> {
            // Get the information...
            final GetTemplateRequest templateRequest = new GetTemplateRequest().withStackName(stackName);
            final AmazonCloudFormationAsyncClient awsClient = new AmazonCloudFormationAsyncClient(super.getAwsCredentials());
            awsClient.setRegion(super.getRegion());
            final GetTemplateResult stackTemplateResult = awsClient.getTemplate(templateRequest);

            // Get the stack info and return it
            final DescribeStacksRequest describeRequest = new DescribeStacksRequest().withStackName(stackName);
            final DescribeStacksResult describeResult = awsClient.describeStacks(describeRequest);

            // And finally the tags and parameters
            final Stack stackInfo = !describeResult.getStacks().isEmpty() ? describeResult.getStacks().get(0) : null;
            final Map<String, Object> tags = (stackInfo != null) ?
                    stackInfo.
                            getTags().
                            stream().
                            collect(Collectors.toMap(Tag::getKey, Tag::getValue)) :
                    Collections.emptyMap();

            final Map<String, Object> params = (stackInfo != null) ?
                    stackInfo.
                            getParameters().
                            stream().
                            collect(Collectors.toMap(Parameter::getParameterKey, Parameter::getParameterValue)) :
                    Collections.emptyMap();


            HashMap<String, Object> results = new HashMap<>();
            results.put("tags", tags);
            results.put("params", params);
            results.put("stack", stackInfo);
            results.put("template", stackTemplateResult.getTemplateBody().toString());
            Gson gson = new Gson();
            return gson.toJson(results);
        };
        engine.put("TargetStackInfoImpl", fnStackInfo);

        Supplier<String> fnArgs = () -> {
            HashMap<String, Map<String, Object>> args = new HashMap<>();
            args.put("arguments", this.arguments);
            Gson gson = new Gson();
            return gson.toJson(args);
        };
        engine.put("ArgumentsImpl", fnArgs);
    }

    @Override
    protected Stream<INashornEvaluationAccumulator> evaluationAccumulators()
    {
        INashornEvaluationAccumulator[] accumulators = new INashornEvaluationAccumulator[]{
                new FileUtils(),
                new LambdaUtils(),
                new CloudFormationPatchUtils()
        };
        return Arrays.stream(accumulators);
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