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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.script.ScriptEngine;

import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.google.gson.Gson;
import com.mweagle.tereus.NashornEvaluator;

public abstract class AWSEvaluationPipeline extends NashornEvaluator
{
	private final AWSCredentials awsCredentials;
	private final Region region;
	private final Logger logger;
	
	protected AWSEvaluationPipeline(final AWSCredentials awsCredentials, final Region awsRegion, final Logger logger)
	{
		this.awsCredentials = awsCredentials;
		this.region = awsRegion;
		this.logger = logger;
	}
	
	protected void publishGlobals(ScriptEngine engine)
	{		
		// Stuff the arguments in there...
		Supplier<String> fnAWSInfo = () -> {
			final Map<String, String> creds = new HashMap<>();
			creds.put("accessKeyId", this.getAwsCredentials().getAWSAccessKeyId());
			creds.put("secretAccessKey", this.getAwsCredentials().getAWSSecretKey());
			
			final Map<String, Object> awsInfo = new HashMap<>();
			awsInfo.put("credentials", creds);
			awsInfo.put("region", this.getRegion().toString());
            Gson gson = new Gson();
            return gson.toJson(awsInfo);
        };
        engine.put("AWSInfoImpl", fnAWSInfo);
		
        // User information
		final AmazonIdentityManagementClient client = new AmazonIdentityManagementClient();	
		final GetUserResult result = client.getUser();
		engine.put("UserInfoImpl", result);
		
        // And the logger
        engine.put("logger", this.logger);	
	}

	protected AWSCredentials getAwsCredentials()
	{
		return awsCredentials;
	}

	protected Region getRegion()
	{
		return region;
	}
	public Logger getLogger()
	{
		return logger;
	}
}
