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

package com.mweagle.tereus.aws;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.*;
import com.google.common.collect.Sets;

import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Predicate;

/**
 * Created by mweagle on 5/8/15.
 */
public class CloudFormation {
		
	protected final static Set<String> TERMINAL_EVENTS  = Collections.unmodifiableSet(Sets.newHashSet("CREATE_FAILED", 
																								"CREATE_COMPLETE", 
																								"DELETE_FAILED",
																								"DELETE_COMPLETE",
																								"UPDATE_FAILED",
																								"UPDATE_COMPLETE",
																								"ROLLBACK_COMPLETE",
																								"ROLLBACK_FAILED"));
	
	
    public Optional<DescribeStacksResult> createStack(final CreateStackRequest request, final Region awsRegion, Logger logger)
    {
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        final AmazonCloudFormationAsyncClient awsClient = new AmazonCloudFormationAsyncClient(credentialProviderChain.getCredentials());
        awsClient.setRegion(awsRegion);
        logger.info("Creating stack: {}", request.getStackName());
        Optional<DescribeStacksResult> completionResult = Optional.empty();

        try
        {
        	// There are no prior events for a creation request
            Future<CreateStackResult> createStackRequest = awsClient.createStackAsync(request);
            final CreateStackResult stackResult = createStackRequest.get();
            logger.info("Stack ({}) creation in progress.", stackResult.getStackId());
            completionResult = waitForStackComplete(awsClient, stackResult.getStackId(), Collections.emptyList(), logger);
        }
        catch (Exception ex)
        {
            logger.error(ex);
        }
        return completionResult;
    }

    public Optional<DescribeStacksResult> updateStack(final UpdateStackRequest request, final Region awsRegion, Logger logger)
    {
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        final AmazonCloudFormationAsyncClient awsClient = new AmazonCloudFormationAsyncClient(credentialProviderChain.getCredentials());
        awsClient.setRegion(awsRegion);
        logger.info("Updating stack: {}", request.getStackName());
        Optional<DescribeStacksResult> completionResult = Optional.empty();

        try
        {
            final List<StackEvent> priorEvents = getStackEvents(awsClient, request.getStackName(), logger);
            logger.info("Total number of pre-existing stack events: {}", priorEvents.size());
            Future<UpdateStackResult> updateStackResult = awsClient.updateStackAsync(request);
            final UpdateStackResult stackResult = updateStackResult.get();
            logger.info("Stack ({}) creation in progress.", stackResult.getStackId());
            completionResult = waitForStackComplete(awsClient, request.getStackName(), priorEvents, logger);
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
    
    protected Optional<DescribeStacksResult> waitForStackComplete(final AmazonCloudFormationAsyncClient awsClient, final String stackName, List<StackEvent> priorEvents, Logger logger) throws Exception
    {
        Map<String, StackEvent> eventHistory = new HashMap<>();
        for (StackEvent eachEvent: priorEvents)
        {
        	eventHistory.put(eachEvent.getEventId(), eachEvent);
        }
        Optional<StackEvent> terminationEvent = Optional.empty();

        final Predicate<StackEvent> isNewEvent = event -> {
            return !eventHistory.containsKey(event.getEventId());
        };

        final Predicate<StackEvent> isTerminalEvent = stackEvent -> {        						
            return (CloudFormation.TERMINAL_EVENTS.contains(stackEvent.getResourceStatus()) &&
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
        
        // Don't ever delete anything, unless the initial event set length was empty, implying
        // a creation event
        if (priorEvents.size() <= 0 && 
        	terminationEvent.get().getResourceStatus().contains("_FAILED"))
        {
            logger.warn("Stack creation . Deleting stack.");
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
