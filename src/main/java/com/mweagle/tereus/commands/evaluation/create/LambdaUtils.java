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
package com.mweagle.tereus.commands.evaluation.create;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.zeroturnaround.zip.ZipUtil;

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

	public String createFunction(final String lambdaSourceRoot, final String bucketName, final String s3KeyName) throws IOException, InterruptedException
	{
		// Install, zip it, and upload it.  Return:
		/*
			{
			  "S3Bucket" : String,
			  "S3Key" : String,
			  "S3ObjectVersion" : String - TODO
			}
		*/

        final String lambdaDir = this.templateRoot.resolve(lambdaSourceRoot).normalize().toAbsolutePath().toString();

        // Is there a package.json file?
        final Path packageJsonPath = Paths.get(lambdaDir, "package.json");
        if (Files.exists(packageJsonPath))
        {
	        // npm install...
			this.logger.info("npm installing Lambda source: {}", lambdaDir);
			try
			{
				Runtime rt = Runtime.getRuntime();
				Process pr = rt.exec("npm install", null, new File(lambdaDir));
				this.logger.info("Waiting for `npm install` to complete");

				final int npmInstallResult = pr.waitFor();
				if (0 != npmInstallResult)
				{
					logger.error("Failed to `npm install`: {}", npmInstallResult);
					throw new IOException("npm install failed for: " + lambdaDir);
				}
			}
			catch (Exception ex)
			{
	        	final String processPath = System.getenv("PATH");
	        	this.logger.error("`npm install` failed. $PATH: {}", processPath);
				throw ex;
			}
        }
        else
        {
        	this.logger.debug("`npm install` bypassed as {} not found", packageJsonPath);
        }
		final Path tempZip = Files.createTempFile("lambda-", ".zip");
		try
		{
			this.logger.info("Zipping lambda source code: {}", tempZip.toString());
			ZipUtil.pack(new File(lambdaDir), tempZip.toFile());
			this.logger.info("Compressed filesize: {} bytes", tempZip.toFile().length());

	        final String keyName = !s3KeyName.isEmpty() ?
	        						s3KeyName :
	        						String.format("%s-tereus-lambda.zip", UUID.randomUUID().toString());
			JsonObject jsonObject = new JsonObject();
			jsonObject.add("S3Bucket", new JsonPrimitive(bucketName));
			jsonObject.add("S3Key", new JsonPrimitive(keyName));

			// Upload it to s3...
			if (!this.dryRun)
			{
				final FileInputStream fis = new FileInputStream(tempZip.toFile());
	            try ( S3Resource resource = new S3Resource(bucketName, keyName, fis, Optional.of(tempZip.toFile().length())))
	            {
	            	Optional<String> result = resource.upload();
	            	this.logger.info("Uploaded Lambda source to: {}", result.get());
	            	resource.setReleased(true);
	            }
			}
			else
			{
	            this.logger.info("Dry run requested (-n/--noop). Lambda payload upload bypassed.");
			}
	        final Gson serializer = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().create();
	        final String stringified = serializer.toJson(jsonObject);
	        return stringified;
		}
		finally
		{
			this.logger.debug("Deleting temporary file: {}", tempZip.toString());
			Files.deleteIfExists(tempZip);
		}
	}
}
