package com.mweagle.tereus.aws;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.*;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Predicate;

/**
 * Created by mweagle on 5/8/15.
 */
public class CloudFormation {

    public Optional<DescribeStacksResult> createStack(final CreateStackRequest request, final Region awsRegion, Logger logger)
    {
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        final AmazonCloudFormationAsyncClient awsClient = new AmazonCloudFormationAsyncClient(credentialProviderChain.getCredentials());
        awsClient.setRegion(awsRegion);
        logger.info("Creating stack: {}", request.getStackName());
        Optional<DescribeStacksResult> completionResult = Optional.empty();

        try
        {
            Future<CreateStackResult> createStackRequest = awsClient.createStackAsync(request);
            final CreateStackResult stackResult = createStackRequest.get();
            logger.info("Stack ({}) creation in progress.", stackResult.getStackId());
            completionResult = waitForStackComplete(awsClient, stackResult.getStackId(), logger);
        }
        catch (Exception ex)
        {
            logger.error(ex);
        }
        return completionResult;
    }

    protected List<StackEvent> getStackEvents(final AmazonCloudFormationAsyncClient awsClient, final String stackName, Logger logger) throws Exception
    {
        List<StackEvent> events = new ArrayList<StackEvent>();
        Optional<String> token = Optional.empty();

        final DescribeStackEventsRequest describeRequest = new DescribeStackEventsRequest();
        describeRequest.setStackName(stackName);
        do {
            if (token.isPresent())
            {
                describeRequest.setNextToken(token.get());
            }
            final Future<DescribeStackEventsResult> stackEvents = awsClient.describeStackEventsAsync(describeRequest);
            DescribeStackEventsResult eventResult = stackEvents.get();
            events.addAll(eventResult.getStackEvents());
            token = Optional.ofNullable(eventResult.getNextToken());
        } while (token.isPresent());
        return events;
    }
    protected Optional<DescribeStacksResult> describeStack(final AmazonCloudFormationAsyncClient awsClient, final String stackName, Logger logger) throws Exception
    {
        final DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackName);
        return Optional.of(awsClient.describeStacks(describeStacksRequest));
    }
    protected Optional<DescribeStacksResult> waitForStackComplete(final AmazonCloudFormationAsyncClient awsClient, final String stackName, Logger logger) throws Exception
    {
        Map<String, StackEvent> eventHistory = new HashMap<>();
        Optional<StackEvent> terminationEvent = Optional.empty();

        final Predicate<StackEvent> isNewEvent = event -> {
            return !eventHistory.containsKey(event.getEventId());
        };

        final Predicate<StackEvent> isTerminalEvent = stackEvent -> {
            return ((stackEvent.getResourceStatus().contains("_COMPLETE")) &&
                    stackEvent.getResourceType().equals("AWS::CloudFormation::Stack"));
        };

        // Query for events
        final DescribeStackEventsRequest describeRequest = new DescribeStackEventsRequest();
        describeRequest.setStackName(stackName);
        while (!terminationEvent.isPresent()) {
            logger.debug("Waiting for StackEvents");
            Thread.sleep(20 * 1000);

            final List<StackEvent> events = getStackEvents(awsClient, stackName, logger);

            // Get all the events we haven't seen, log and mark them
            events.stream().filter(isNewEvent).forEach(item -> {
                logger.info(item.toString());
                eventHistory.put(item.getEventId(), item);
            });

            // Find the first terminal event
            terminationEvent = events.stream().filter(isTerminalEvent).findFirst();
        }
        if (!terminationEvent.get().getResourceStatus().equals("CREATE_COMPLETE"))
        {
            logger.warn("Stack creation failed. Deleting stack.");
            final DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
            deleteStackRequest.setStackName(stackName);
            awsClient.deleteStack(deleteStackRequest);
            return Optional.empty();
        }
        else
        {
            // Looks good, let's get the final output for the stack...
            return describeStack(awsClient, stackName, logger);
        }
    }
}
