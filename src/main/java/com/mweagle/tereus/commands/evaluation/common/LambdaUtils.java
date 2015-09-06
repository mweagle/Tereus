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
package com.mweagle.tereus.commands.evaluation.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.zip.ZipUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mweagle.tereus.INashornEvaluationAccumulator;
import com.mweagle.tereus.INashornEvaluatorContext;
import com.mweagle.tereus.aws.S3Resource;

public class LambdaUtils implements INashornEvaluationAccumulator {

    private final Path templateRoot;
    private final boolean dryRun;
    private final Logger logger;
    public LambdaUtils(INashornEvaluatorContext context)
    {
        this.templateRoot = context.getEvaluationSource().getParent();
        this.dryRun = context.isDryRun();
        this.logger = context.getLogger();
    }

	@Override
	public String getAccumulatorName() {
		return "LambdaUtilsImpl";
	}
	
	public String createFunction(final String lambdaSourceRoot, 
									final String bucketName, 
									final String s3KeyName) throws IOException, InterruptedException
	{
		// Build it, zip it, and upload it.  Return:
		/*
			{
			  "S3Bucket" : String,
			  "S3Key" : String,
			  "S3ObjectVersion" : "TODO - not yet implemented"
			}
		*/

        final String lambdaDir = this.templateRoot.resolve(lambdaSourceRoot).normalize().toAbsolutePath().toString();
        
        // Build command?
        final Optional<String> buildCommand = lambdaBuildCommand(lambdaDir);
        if (buildCommand.isPresent())
        {
			this.logger.info("{} Lambda source: {}", buildCommand.get(), lambdaDir);
			try
			{
				Runtime rt = Runtime.getRuntime();
				Process pr = rt.exec(buildCommand.get(), null, new File(lambdaDir));
				this.logger.info("Waiting for `{}` to complete", buildCommand.get());

				final int buildExitCode = pr.waitFor();
				if (0 != buildExitCode)
				{
					logger.error("Failed to `{}`: {}", buildCommand.get(), buildExitCode);
					throw new IOException(buildCommand.get() + " failed for: " + lambdaDir);
				}
			}
			catch (Exception ex)
			{
	        	final String processPath = System.getenv("PATH");
	        	this.logger.error("`{}` failed. Confirm that PATH contains the required executable.", buildCommand.get());
	        	this.logger.error("$PATH: {}", processPath);
				throw ex;
			}
        }
        else
        {
        	this.logger.debug("No additional Lambda build file detected");
        }
        
		Path lambdaSource = null;
		boolean cleanupLambdaSource = false;
		
		try
		{
			final BiPredicate<Path, java.nio.file.attribute.BasicFileAttributes> matcher =  (path, fileAttrs) -> {
				final String fileExtension = com.google.common.io.Files.getFileExtension(path.toString());
				return (fileExtension.toLowerCase().compareTo("jar") == 0);
			};
			
			// If there is a JAR file in the source root, then use that for the upload
			List<Path> jarFiles = Files.find(Paths.get(lambdaDir), 1, matcher).collect(Collectors.toList());
				
			if (!jarFiles.isEmpty())
			{
				Preconditions.checkArgument(jarFiles.size() == 1, "More than 1 JAR file detected in directory: {}", lambdaDir);
				lambdaSource = jarFiles.get(0);
			}
			else
			{
				// TODO - use java.util.zip to create stable source package
				//http://stackoverflow.com/questions/23612864/create-a-zip-file-in-memory
				lambdaSource = Files.createTempFile("lambda-", ".zip");
				this.logger.info("Zipping lambda source code: {}", lambdaSource.toString());
				ZipUtil.pack(new File(lambdaDir), lambdaSource.toFile());
				this.logger.info("Compressed filesize: {} bytes", lambdaSource.toFile().length());
				cleanupLambdaSource = true;
			}
			
			// Get the hash of the source contents
	        final String sourceHash =  DigestUtils.sha256Hex(Files.newInputStream(lambdaSource)); 
	        if (!s3KeyName.isEmpty())
	        {
	        	this.logger.warn("User supplied S3 keyname overrides content-addressable name. Automatic updates disabled.");
	        }
	        final String keyName = !s3KeyName.isEmpty() ? 
	        						s3KeyName : 
        							String.format("%s-%s.%s", 
        										com.google.common.io.Files.getNameWithoutExtension(lambdaSource.toString()),	
        										sourceHash,
        										com.google.common.io.Files.getFileExtension(lambdaSource.toString()));
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("S3Bucket", new JsonPrimitive(bucketName));
			jsonObject.add("S3Key", new JsonPrimitive(keyName));			
			
			// Upload it to s3...
			final FileInputStream fis = new FileInputStream(lambdaSource.toFile());
            try ( S3Resource resource = new S3Resource(bucketName, keyName, fis, Optional.of(lambdaSource.toFile().length())))
            {
    	        this.logger.info("Source payload S3 URL: {}", resource.getS3Path());

            	if (resource.exists())
            	{
            		this.logger.info("Source {} already uploaded to S3", keyName);
            	}
            	else if (!this.dryRun)
            	{
	            	Optional<String> result = resource.upload();
	            	this.logger.info("Uploaded Lambda source to: {}", result.get());
	            	resource.setReleased(true);	            		
            	}
            	else
            	{
    	            this.logger.info("Dry run requested (-n/--noop). Lambda payload upload bypassed.");
            	}
            }
	        final Gson serializer = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().create();
	        final String stringified = serializer.toJson(jsonObject);
	        return stringified;
		}
		finally
		{
			if (cleanupLambdaSource && (null != lambdaSource))
			{
				this.logger.debug("Deleting temporary file: {}", lambdaSource.toString());
				Files.deleteIfExists(lambdaSource);				
			}
		}
	}
	
	protected Optional<String> lambdaBuildCommand(final String lambdaRootDir)
	{			
		final Map<String, String> builderMap = new ImmutableMap.Builder<String, String>()
										        .put("package.json", "npm install")
										        .put("build.xml", "ant")
										        .put("build.gradle", "gradle build")
										        .put("pom.xml","mvn clean deploy")
										        .build();
		
		Optional<Map.Entry<String, String>> commandPair = builderMap.entrySet().stream().filter(eachPair ->
			Files.exists(Paths.get(lambdaRootDir, eachPair.getKey()))
		).findFirst();
		return Optional.ofNullable(commandPair.isPresent() ? commandPair.get().getValue() : null);
	}
}