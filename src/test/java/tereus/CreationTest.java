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
package tereus;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mweagle.TereusInput;
import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.commands.CreateCommand;

/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(org.junit.runners.JUnit4.class)
public class CreationTest  {
    final protected Logger logger = LogManager.getLogger(CreationTest.class.getName());

	private static Path lambdaDefinitionPath()
	{				
		return Paths.get(TestUtils.TestRoot().toString(), 
						 "evaluation",
						 "aws_samples",
						 "definition",
						 "Lambda.js").toAbsolutePath();
	};
	
	
    @Test
    public void test() throws Exception {
    	final String createStack = System.getenv("JUNIT_CREATE_STACK");
    	if (System.getenv().containsKey("JUNIT_CREATE_STACK") && Boolean.parseBoolean(createStack))
    	{
        	// Create the Lambda stack
            Map<String, Object> params = new HashMap<>();
            params.put(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, "weagle");

            final TereusInput input = new TereusInput(null, CreationTest.lambdaDefinitionPath().toString(), "us-west-2", params, new HashMap<String, Object>(), false);

            Optional<ByteArrayOutputStream> os = Optional.of(new ByteArrayOutputStream());
            CreateCommand tereus = new CreateCommand();
            tereus.create(input, os);
            final JsonElement evaluated = new JsonParser().parse(os.get().toString("UTF-8"));
            final String evaluatedString = new GsonBuilder().create().toJson(evaluated);
    		logger.info(evaluatedString);
    	}
    	else
    	{
    		logger.warn("Creation test bypassed.  Set env.JUNIT_CREATE_STACK=true to enable");
    	}
    }
}
