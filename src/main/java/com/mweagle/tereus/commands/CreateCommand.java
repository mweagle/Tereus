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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.EstimateTemplateCostRequest;
import com.amazonaws.services.cloudformation.model.EstimateTemplateCostResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest;
import com.amazonaws.services.cloudformation.model.ValidateTemplateResult;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.aws.CloudFormation;
import com.mweagle.tereus.aws.S3Resource;
import com.mweagle.tereus.commands.pipelines.CreationPipeline;
import com.mweagle.tereus.input.TereusInput;

import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.Option;

@Command(name = "create", description = "Create a CloudFormation stack")
public class CreateCommand extends AbstractTereusAWSCommand
{
	@Option(name = { "-t",
			"--template" }, description = "Path to CloudFormation definition [REQUIRED]", required = true)
	public String stackTemplatePath;

	@Option(name = { "-a",
			"--arguments" }, description = "Path to JSON file including \"Parameters\" & \"Tags\" values")
	public String jsonParamAndTagsPath;

	@Option(name = { "-b",
			"--bucket" }, description = "S3 Bucketname to host stack resources. MUST be CLI option OR `Parameters.BucketName` value in JSON input")
	public String s3BucketName;

	@Option(name = { "-s",
			"--stack" }, description = "Optional Stack Name to use.  If empty, {basename+SHA256(templateData)} will be provided")
	public String stackName;

	@Option(name = { "-o", "--output" }, description = "Optional file to which evaluated template will be saved")
	public String outputFilePath;
	
	
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
        "DM_EXIT", 
        "OBL_UNSATISFIED_OBLIGATION"})
	@SuppressWarnings("unchecked")
	@Override
	public void run()
	{
		int exitCode = 0;
		try
		{
			final String argumentJSON = (null != this.jsonParamAndTagsPath)
					? new String(Files.readAllBytes(Paths.get(this.jsonParamAndTagsPath)), Charsets.UTF_8) : null;

			Map<String, Object> jsonJavaRootObject = (null != argumentJSON)
					? new Gson().fromJson(argumentJSON, Map.class) : Collections.emptyMap();
			Map<String, Object> parameters = (Map<String, Object>) jsonJavaRootObject
					.getOrDefault(CONSTANTS.ARGUMENT_JSON_KEYNAMES.PARAMETERS, new HashMap<>());
			final String jsonS3BucketName = ((String) parameters.getOrDefault(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME,
					"")).trim();
			final String cliS3BucketName = (null == this.s3BucketName) ? "" : this.s3BucketName.trim();

			if (!jsonS3BucketName.isEmpty() && !cliS3BucketName.isEmpty())
			{
				final String msg = String.format("S3 bucketname defined in both %s and via command line argument",
						this.jsonParamAndTagsPath);
				throw new IllegalArgumentException(msg);
			} else if (!cliS3BucketName.isEmpty())
			{
				parameters.put(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, cliS3BucketName);
			}

			Map<String, Object> tags = (Map<String, Object>) jsonJavaRootObject
					.getOrDefault(CONSTANTS.ARGUMENT_JSON_KEYNAMES.TAGS, Collections.emptyMap());

			TereusInput tereusInput = new TereusInput(this.stackName, this.stackTemplatePath, this.region, parameters,
					tags, this.dryRun);

			Optional<OutputStream> osSink = Optional.empty();
			try
			{
				if (null != this.outputFilePath)
				{
					final Path outputPath = Paths.get(this.outputFilePath);
					osSink = Optional.of(new FileOutputStream(outputPath.toFile()));
				}
				this.create(tereusInput, osSink);
			} catch (Exception ex)
			{
				LogManager.getLogger().error(ex);
				exitCode = 2;
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
		} catch (Exception ex)
		{
			LogManager.getLogger().error(ex);
			Help.help(this.helpOption.commandMetadata);
			exitCode = 1;
		}
		System.exit(exitCode);
	}

	public Map<String, Object> create(final TereusInput tereusInput, Optional<? extends OutputStream> osSinkTemplate) throws Exception
	{
		final CreationPipeline pipeline = new CreationPipeline(tereusInput);
		Map<String, Object> evaluationResult = pipeline.run(tereusInput.stackDefinitionPath, tereusInput.logger);
				
		final Optional<Object> templateData = Optional.ofNullable(evaluationResult.get("Template"));
		final Optional<Object> prepopulatedTemplate = Optional
				.ofNullable(evaluationResult.get("ParameterizedTemplate"));

		if (templateData.isPresent())
		{
			// It's possible there's a command line option that overrides the
			// stack name
			if (null == this.stackName)
			{
				final String stackBaseName = (String) (evaluationResult.get("StackName"));
				final String templateString = new GsonBuilder().create().toJson(templateData.get());
				this.stackName = String.format("%s-%s", stackBaseName, DigestUtils.sha256Hex(templateString));
			}
		}

		// Upload the parameterized template to S3, validate it, and cleanup
		validateTemplate(tereusInput,
				new GsonBuilder().disableHtmlEscaping().create().toJson(prepopulatedTemplate.get()));

		// Create the stack
		createStack(tereusInput, (JsonElement) templateData.get(), !osSinkTemplate.isPresent());

		if (osSinkTemplate.isPresent())
		{
			final String formattedTemplate = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
					.toJson(templateData.get());
			osSinkTemplate.get().write(formattedTemplate.getBytes(Charset.forName("UTF-8")));
		}
		return evaluationResult;
	}

	protected List<Parameter> toParameterList(final Map<String, Object> values)
	{
		return values.entrySet().stream().map(eachEntry -> {
			Parameter awsParam = new Parameter();
			awsParam.setParameterKey(eachEntry.getKey());
			awsParam.setParameterValue(eachEntry.getValue().toString());
			return awsParam;
		}).collect(Collectors.toList());
	}

	protected List<Tag> toTagList(final Map<String, Object> values)
	{
		return values.entrySet().stream().map(eachEntry -> {
			Tag awsTag = new Tag();
			awsTag.setKey(eachEntry.getKey());
			awsTag.setValue(eachEntry.getValue().toString());
			return awsTag;
		}).collect(Collectors.toList());
	}

	protected void createStack(TereusInput tereusInput, JsonElement templateData, boolean logTemplate)
			throws UnsupportedEncodingException
	{
		if (tereusInput.dryRun)
		{
			tereusInput.logger.info("Dry run requested (-n/--noop). Stack creation bypassed.");
			if (logTemplate)
			{
				final String formattedTemplate = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
						.toJson(templateData);
				tereusInput.logger.info("Stack Template:\n {}", formattedTemplate);
			}
		} else
		{
			final String bucketName = tereusInput.params.get(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME).toString();
			// Upload the template
			final String templateContent = new GsonBuilder().create().toJson(templateData);
			final byte[] templateBytes = templateContent.getBytes("UTF-8");
			final InputStream is = new ByteArrayInputStream(templateBytes);
			final String templateDigest = DigestUtils.sha256Hex(templateBytes);
			final String keyName = String.format("%s-tereus.cf.template", templateDigest);

			try (S3Resource resource = new S3Resource(bucketName, keyName, is,
					Optional.of(Long.valueOf(templateBytes.length))))
			{
				resource.upload();
				final EstimateTemplateCostRequest costRequest = new EstimateTemplateCostRequest();
				costRequest.setParameters(toParameterList(tereusInput.params));
				costRequest.setTemplateURL(resource.getResourceURL().get());
				final AmazonCloudFormationClient awsClient = new AmazonCloudFormationClient(tereusInput.awsCredentials);
				awsClient.setRegion(tereusInput.awsRegion);
				final EstimateTemplateCostResult costResult = awsClient.estimateTemplateCost(costRequest);
				tereusInput.logger.info("Cost Estimator: {}", costResult.getUrl());

				// Go ahead and create the stack.
				final CreateStackRequest request = new CreateStackRequest().withStackName(stackName)
						.withTemplateURL(resource.getResourceURL().get())
						.withParameters(toParameterList(tereusInput.params)).withTags(toTagList(tereusInput.tags))
						.withCapabilities("CAPABILITY_IAM");
				tereusInput.logger.debug("Creating stack: {}", stackName);
				tereusInput.logger.debug("Stack params: {}", request.getParameters());
				tereusInput.logger.debug("Stack tags: {}", request.getTags());
				final Optional<DescribeStacksResult> result = new CloudFormation().createStack(request,
						tereusInput.awsRegion, tereusInput.logger);
				if (result.isPresent())
				{
					tereusInput.logger.info("Stack successfully created");
					tereusInput.logger.info(result.get().toString());
					resource.setReleased(true);
				}
			}
		}
	}

	protected void validateTemplate(TereusInput tereusInput, String parameterizedTemplate)
			throws UnsupportedEncodingException
	{
		if (tereusInput.dryRun)
		{
			tereusInput.logger.info("Dry run requested (-n/--noop). Stack validation bypassed.");
		} else
		{
			tereusInput.logger.info("Validating template with AWS");
			final String bucketName = tereusInput.params.get(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME).toString();
			final byte[] templateBytes = parameterizedTemplate.getBytes("UTF-8");
			final InputStream is = new ByteArrayInputStream(templateBytes);

			final String templateDigest = DigestUtils.sha256Hex(templateBytes);
			final String keyName = String.format("%s-tereus-pre.cf.template", templateDigest);
			try (S3Resource resource = new S3Resource(bucketName, keyName, is,
					Optional.of(Long.valueOf(templateBytes.length))))
			{
				Optional<String> templateURL = resource.upload();
				final ValidateTemplateRequest validationRequest = new ValidateTemplateRequest();
				validationRequest.setTemplateURL(templateURL.get());
				final AmazonCloudFormationClient awsClient = new AmazonCloudFormationClient(tereusInput.awsCredentials);
				awsClient.setRegion(tereusInput.awsRegion);
				final ValidateTemplateResult validationResult = awsClient.validateTemplate(validationRequest);
				tereusInput.logger.debug("Stack template validation results:");
				tereusInput.logger.debug(validationResult.toString());
			}
		}
	}
}
