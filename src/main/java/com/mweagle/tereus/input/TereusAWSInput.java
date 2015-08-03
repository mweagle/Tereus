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
package com.mweagle.tereus.input;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class TereusAWSInput
{
    public final Region awsRegion;
    public final boolean dryRun;
    public final AWSCredentials awsCredentials;
    public final Logger logger;

    public TereusAWSInput(final String awsRegion, final boolean dryRun, final String loggerName)
    {
        this.awsRegion = (null != awsRegion) ?
                Region.getRegion(Regions.fromName(awsRegion)):
                Region.getRegion(Regions.US_EAST_1);
        this.dryRun = dryRun;
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        this.awsCredentials = credentialProviderChain.getCredentials();
        this.logger = LogManager.getLogger(loggerName);
    }

    protected boolean isValidPathArgument(String argument)
    {
        return (null != argument &&
        		!argument.isEmpty() &&
                Files.exists(Paths.get(argument)) &&
                !Files.isDirectory(Paths.get(argument)));
    }

    protected boolean isNonEmptyString(String argument)
    {
        return (null != argument &&
        		!argument.trim().isEmpty());
    }
}
