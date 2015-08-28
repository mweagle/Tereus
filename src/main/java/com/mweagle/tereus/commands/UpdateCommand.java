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

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.google.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.aws.CloudFormation;
import com.mweagle.tereus.aws.S3Resource;
import com.mweagle.tereus.commands.pipelines.UpdatePipeline;
import com.mweagle.tereus.input.UpdateInput;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "update", description = "Update a CloudFormation stack by name or ID via a JSON Patch (RFC 6902)")
public class UpdateCommand extends AbstractTereusAWSCommand
{
	@Option(name = { "-p", "--patch" }, description = "Path to CloudFormation patch definition file [REQUIRED]", required = true)
	public String patchDefinitionPath;
	
	@Option(name = { "-s", "--stack" }, description = "StackName or StackId to update")
	public String stackName;

	@Option(name = { "-a", "--arg" },arity = 2, description = "Name-value argument pair. Published as ARGUMENTS in JSON Patch evaluation")
	public List<String> arguments;

	@Option(name = { "-o", "--output" }, description = "Optional file to which evaluated template will be saved")
	public String outputFilePath;
	
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
        "DM_EXIT", 
        "OBL_UNSATISFIED_OBLIGATION"})
	@Override
	public void run()
	{
		Map<String, Object> argumentMap = new HashMap<>();
		Optional<OutputStream> osSink = Optional.empty();
		int exitCode = 0;
		final UpdateInput updateInput = new UpdateInput(this.patchDefinitionPath, argumentMap, this.stackName, this.region, this.dryRun);
		try
		{
			if (null != this.outputFilePath)
			{
				final Path outputPath = Paths.get(this.outputFilePath);
				osSink = Optional.of(new FileOutputStream(outputPath.toFile()));
			}
			this.update(updateInput, osSink);
		} catch (Exception ex)
		{
			LogManager.getLogger().error(ex);
			exitCode = 1;
		} 
		finally
		{
			if (osSink.isPresent())
			{
				try
				{
					osSink.get().close();
				} catch (Exception e)
				{
					// NOP
				}
			}
		}
		System.exit(exitCode);
	}
	
	public Map<String, Object> update(final UpdateInput input, Optional<? extends OutputStream> osSinkTemplate) throws Exception
	{
		final UpdatePipeline pipeline = new UpdatePipeline(input.patchPath.getParent(),
															input.arguments,
															input.stackTemplateResult,
															input.awsCredentials,
															input.awsRegion,
															input.dryRun,
															input.logger);
				
		Map<String, Object> evaluationResult = pipeline.run(input.patchPath, input.logger);
		final Optional<Object> patchData = Optional.ofNullable(evaluationResult.get("Patch"));
		if (osSinkTemplate.isPresent())
		{
			final String formattedTemplate = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
					.toJson(patchData.get());
			osSinkTemplate.get().write(formattedTemplate.getBytes(Charset.forName("UTF-8")));
		}

		updateStack(input, evaluationResult.get("Result").toString());
		
		// Wait
		return evaluationResult;
	}
	
	
	protected Optional<Parameter> findNamedParameter(final String paramName, final List<Parameter> params)
	{
		return params.stream().filter(eachParameter ->
			(0 == eachParameter.getParameterKey().compareTo(paramName))
		).findFirst();
	}
	
	protected void updateStack(UpdateInput updateInput, String transformedTemplate)
			throws UnsupportedEncodingException
	{
		if (updateInput.dryRun)
		{
			updateInput.logger.info("Dry run requested (-n/--noop). Stack update bypassed.");
		} 
		else
		{
			// Fetch the stack parameters
			final DescribeStacksRequest stackRequest = new DescribeStacksRequest().withStackName(updateInput.stackName);
	        final AmazonCloudFormationAsyncClient awsClient = new AmazonCloudFormationAsyncClient(updateInput.awsCredentials);
	        awsClient.setRegion(updateInput.awsRegion);
	        final DescribeStacksResult result = awsClient.describeStacks(stackRequest);
	        final Stack existantStack = result.getStacks().get(0);
	        
	        final Optional<Parameter> s3Bucket = findNamedParameter(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME,
	        														existantStack.getParameters());

	        Preconditions.checkArgument(s3Bucket.isPresent(), 
	        		"Failed to determine S3 BucketName from existant template via parameter name: " + CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME);
	        
	        // Super, now put the new content to S3, update the parameter list
	        // to include the new URL, and submit the updated stack.
			final byte[] templateBytes = transformedTemplate.getBytes("UTF-8");
			final InputStream is = new ByteArrayInputStream(templateBytes);
			final String templateDigest = DigestUtils.sha256Hex(templateBytes);
			final String keyName = String.format("%s-tereus.cf.template", templateDigest);
			
			// TODO - verify that update stack actually works
			try (S3Resource resource = new S3Resource(s3Bucket.get().getParameterValue(), keyName, is,
					Optional.of(Long.valueOf(templateBytes.length))))
			{
				// Upload the template
				resource.upload();

				// Go ahead and create the stack.
				final UpdateStackRequest request = new UpdateStackRequest().withStackName(stackName);
				request.setTemplateURL(resource.getResourceURL().get());
				request.setParameters(existantStack.getParameters());
				request.setCapabilities(Arrays.asList("CAPABILITY_IAM"));

				updateInput.logger.debug("Updating stack: {}", stackName);
				final Optional<DescribeStacksResult> updateResult = new CloudFormation().updateStack(request,
						updateInput.awsRegion, updateInput.logger);
				
				// If everything worked out, then release the template
				// URL s.t. subsequent ASG instantiated instances have access
				// to the template content
				if (updateResult.isPresent())
				{
					updateInput.logger.info("Stack successfully updated");
					updateInput.logger.info(updateResult.get().toString());
					resource.setReleased(true);
				}
			}
		}
	}
}
