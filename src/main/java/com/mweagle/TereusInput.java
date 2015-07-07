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
package com.mweagle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.base.Preconditions;
import com.mweagle.tereus.CONSTANTS;

/**
 * Created by mweagle on 5/8/15.
 */
public class TereusInput {
    public final String stackName;
    public final Path stackDefinitionPath;
    public final Region awsRegion;
    public final Map<String, Object> params;
    public final Map<String, Object> tags;
    public final boolean dryRun;
    public final AWSCredentials awsCredentials;
    public final Logger logger;

    public TereusInput(String stackName,
                       String stackDefinitionPath,
                       final String awsRegion,
                       final Map<String, Object> cliParams,
                       final Map<String, Object> cliTags,
                       boolean dryRun)
    {
        Preconditions.checkArgument(isValidPathArgument(stackDefinitionPath), "Please provide a stack definition path");
        Preconditions.checkNotNull(cliParams.get(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME),
                "Please provide the S3 bucketname in the JSON Parameters object (%s.%s)",
                CONSTANTS.ARGUMENT_JSON_KEYNAMES.PARAMETERS,
                CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME);
        Preconditions.checkArgument(!((String)cliParams.get(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME)).isEmpty(),
                "Please provide the S3 bucketname in the JSON Parameters object (%s.%s)",
                CONSTANTS.ARGUMENT_JSON_KEYNAMES.PARAMETERS,
                CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME);

        this.stackName = stackName;
        this.stackDefinitionPath = Paths.get(stackDefinitionPath);
        this.params = cliParams;//Collections.unmodifiableMap(listToMap(cliParams));
        this.tags = cliTags;//Collections.unmodifiableMap(listToMap(cliTags));
        this.dryRun = dryRun;
        this.logger = LogManager.getLogger(Tereus.class.getName());

        // AWS Credentials
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        this.awsCredentials = credentialProviderChain.getCredentials();
        this.awsRegion = (null != awsRegion) ?
                            Region.getRegion(Regions.fromName(awsRegion)):
                            Region.getRegion(Regions.US_EAST_1);

        // Log everything
        this.logInput();
    }
    protected boolean isValidPathArgument(String argument)
    {
        return (null != argument &&
        		!argument.isEmpty() &&
                Files.exists(Paths.get(argument)) &&
                !Files.isDirectory(Paths.get(argument)));
    }
    protected void logInput()
    {
        this.logger.info("Tereus Inputs:");
        this.logger.info("\tStack Name: {}", this.stackName);
        this.logger.info("\tStack Definition: {}", this.stackDefinitionPath);
        this.logger.info("\tAWS Region: {}", this.awsRegion);
        this.logger.info("\tTemplate Parameters: {}", this.params);
        this.logger.info("\tStack Tags: {}", this.tags);
        this.logger.info("\tDryRun: {}", this.dryRun);
    }

    protected Map<String, String> listToMap(final List<String> input)
    {
        Map<String, String> results = new HashMap<>();

        for (int i = 0; i != input.size(); i+=2)
        {
            final String key = input.get(i);
            final String value = input.get(i+1);
            results.put(key, value);
        }
        return (input.size() > 0) ?
                results : Collections.emptyMap();
    }
}
