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
package tereus.create;

//import java.io.File;
//import java.util.Map;
import tereus.TestUtils;
import tereus.EvaluationTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.commands.CreateCommand;
import com.mweagle.tereus.input.TereusInput;


/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(Parameterized.class)
public class SingleCreationTest extends EvaluationTest {
	final public static String TEST_TYPE = "aws_samples";
	final public static String TEST_NAME = "EC2Builder";
	
	
    @Parameters
    public static List<Object[]> data() throws IOException {
    	
    	final Path inputPath = Paths.get(TestUtils.testRoot().toString(), 
				 "create",
				 TEST_TYPE,
				 "definition",
				 TEST_NAME + ".js").toAbsolutePath();
    	
    	final Path resultPath = Paths.get(TestUtils.testRoot().toString(), 
				 "evaluation",
				 TEST_TYPE,
				 "expected",
				 TEST_NAME + ".json").toAbsolutePath();
    	return TestUtils.singleDataPair(inputPath, resultPath);
    }
	
	
    @Override
    protected  void run(Path evaluationInput, Optional<ByteArrayOutputStream> evaluationResults) throws Exception
    {
        Map<String, Object> params = new HashMap<>();
        params.put(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, "testBucket");

        final TereusInput input = new TereusInput(null, evaluationFilepath.toString(), null, params, new HashMap<String, Object>(), true);
        new CreateCommand().create(input, evaluationResults);
    }
}
