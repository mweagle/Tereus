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
    public final Path dockerFilePath;
    public final Region awsRegion;
    public final Map<String, Object> params;
    public final Map<String, Object> tags;
    public final boolean dryRun;
    public final AWSCredentials awsCredentials;
    public final Logger logger;

    public TereusInput(String stackName,
                       String stackDefinitionPath,
                       String dockerFilePath,
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
        this.dockerFilePath = (null != dockerFilePath) ? Paths.get(dockerFilePath) : null;
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
        this.logger.info("\tDockerFile Path: {}", this.dockerFilePath);
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
