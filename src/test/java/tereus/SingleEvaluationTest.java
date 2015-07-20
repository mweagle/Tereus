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

//import java.io.File;
//import java.util.Map;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(org.junit.runners.JUnit4.class)
public class SingleEvaluationTest extends EvaluationTest {
		
	private static Path inputPath(final String testType, final String fileRelativePath)
	{		
		return Paths.get(TestUtils.testRoot().toString(), 
						 "evaluation",
						 testType,
						 "definition",
						 fileRelativePath + ".js").toAbsolutePath();
	};
	
	private static Path resultPath(final String testType, final String fileRelativePath)
	{
		return Paths.get(TestUtils.testRoot().toString(), 
				 "evaluation",
				 testType,
				 "expected",
				 fileRelativePath + ".json").toAbsolutePath();
	};	
	
    @Test
    public void test() throws Exception {
    	final String testType = "aws_samples";
    	final String testName = "EC2Builder";
    	final Path inputFile = SingleEvaluationTest.inputPath(testType, testName);
    	final Path expectedFile = SingleEvaluationTest.resultPath(testType, testName);
    	super.verifyEvaluation(inputFile, expectedFile);
    }
}
