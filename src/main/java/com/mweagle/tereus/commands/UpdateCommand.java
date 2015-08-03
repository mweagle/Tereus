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

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;

import com.google.gson.GsonBuilder;
import com.mweagle.tereus.commands.create.UpdatePipeline;
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
		for (int i = 0; i < arguments.size(); i += 2)
		{
			Object argumentValue = arguments.get(i+1);
			try
			{
				argumentValue = Integer.parseInt(arguments.get(i+1));
			}
			catch (Exception ex)
			{
				// NOP
			}
			argumentMap.put(arguments.get(i), argumentValue);
		}
		Optional<OutputStream> osSink = Optional.empty();
		int exitCode = 0;
		final UpdateInput updateInput = new UpdateInput(this.patchDefinitionPath, argumentMap, this.stackName, this.region, this.noop);
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
															input.stackName,
															input.awsCredentials,
															input.awsRegion,
															input.dryRun,
															input.logger);
				
		Map<String, Object> evaluationResult = pipeline.run(input.patchPath, input.logger);
		final Optional<Object> patchData = Optional.ofNullable(evaluationResult.get("Patch"));
		final String patchName = evaluationResult.get("PatchName").toString();
		if (osSinkTemplate.isPresent())
		{
			final String formattedTemplate = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
					.toJson(patchData.get());
			osSinkTemplate.get().write(formattedTemplate.getBytes(Charset.forName("UTF-8")));
		}

		
		// Apply the update
		
		
		// Wait
		return evaluationResult;
	}
    public static void main(String[] args) throws Exception {   
    	
    	final UpdateInput input = new UpdateInput("/Users/mweagle/Documents/GitHub/Tereus/patch.js",
    												Collections.emptyMap(),
    												"asdfasdf",
    												null, 
    												true);
    	
    	final Path outputPath = Paths.get("/Users/mweagle/Documents/GitHub/Tereus/patch.log");
		final Optional<OutputStream> osSink = Optional.of(new FileOutputStream(outputPath.toFile()));
    	new UpdateCommand().update(input, osSink);
    }

}
