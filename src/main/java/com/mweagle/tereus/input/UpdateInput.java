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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.google.common.base.Preconditions;

public class UpdateInput extends TereusAWSInput
{
	final public Path patchPath;
	final public Map<String, Object> arguments;
	final public String stackName;
	final public GetTemplateResult stackTemplateResult;
	
	public UpdateInput(final String patchPath, Map<String, Object> arguments, final String stackName, String awsRegion, boolean dryRun) {
		super(awsRegion, dryRun, UpdateInput.class.getName());
        Preconditions.checkArgument(isValidPathArgument(patchPath), "Please provide a patch definition path");
		this.patchPath = Paths.get(patchPath);
		this.arguments = arguments;
		this.stackName = stackName;
		
		if (null != stackName && !stackName.isEmpty())
		{
			final GetTemplateRequest templateRequest = new GetTemplateRequest().withStackName(stackName);
	        final AmazonCloudFormationAsyncClient awsClient = new AmazonCloudFormationAsyncClient(super.awsCredentials);
	        awsClient.setRegion(super.awsRegion);
	        this.stackTemplateResult = awsClient.getTemplate(templateRequest);
		}
		else
		{
			this.stackTemplateResult = null;
		}
	}
}