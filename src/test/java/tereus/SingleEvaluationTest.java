package tereus;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(org.junit.runners.JUnit4.class)
public class SingleEvaluationTest extends EvaluationTest {
		
	private static String TEST_FILES_ROOT = System.getenv("JUNIT_TEST_ROOT");

	private static Path inputPath(final String testType, final String fileRelativePath)
	{		
		return Paths.get(SingleEvaluationTest.TEST_FILES_ROOT, 
						 "evaluation",
						 testType,
						 "definition",
						 fileRelativePath + ".js").toAbsolutePath();
	};
	
	private static Path resultPath(final String testType, final String fileRelativePath)
	{
		return Paths.get(SingleEvaluationTest.TEST_FILES_ROOT, 
				 "evaluation",
				 testType,
				 "expected",
				 fileRelativePath + ".json").toAbsolutePath();
	};	
	
    @Test
    public void test() throws Exception {
    	if (System.getenv().containsKey("JUNIT_TEST_ROOT"))
    	{
        	final String testType = "aws_samples";
        	final String testName = "EC2Builder";
        	final Path inputFile = SingleEvaluationTest.inputPath(testType, testName);
        	final Path expectedFile = SingleEvaluationTest.resultPath(testType, testName);
        	super.verifyEvaluation(inputFile, expectedFile);	
    	}
    }
}
