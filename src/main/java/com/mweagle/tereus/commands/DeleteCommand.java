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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "delete", description = "Delete a CloudFormation stack by Name or Id")
public class DeleteCommand extends AbstractTereusAWSCommand
{
    @Option(name = { "-s",
    "--stack" }, description = "StackName or StackId to delete")
    public String stackName;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
        "DM_EXIT", 
        "OBL_UNSATISFIED_OBLIGATION"})
    @Override
    public void run()
    {
        // Get the stack, delete the stack...
        int exitCode = 0;
        final Logger logger = LogManager.getLogger();

        try
        {
            final AmazonCloudFormationClient awsClient = new AmazonCloudFormationClient();
            awsClient.setRegion(RegionUtils.getRegion(this.region));
            final DescribeStacksRequest describeRequest = new DescribeStacksRequest().withStackName(this.stackName);
            final DescribeStacksResult describeResult = awsClient.describeStacks(describeRequest);
            logger.info(describeResult);
            logger.info("Deleting stack: {}", this.stackName);

            if (this.dryRun)
            {
                logger.info("Dry run requested (-n/--noop). Stack deletion bypassed.");
            }
            else
            {
                final DeleteStackRequest deleteRequest = new DeleteStackRequest().withStackName(this.stackName);
                awsClient.deleteStack(deleteRequest);
            }
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
            exitCode = 1;
        }
        System.exit(exitCode);
    }

}
