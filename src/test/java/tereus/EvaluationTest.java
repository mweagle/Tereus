package tereus;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public abstract class EvaluationTest
{
    final protected Logger logger = LogManager.getLogger(EvaluationTest.class.getName());

    @Parameter(0)
    public Path evaluationFilepath;
    @Parameter(1)
    public Path expectedResultPath;
    
    abstract protected void run(Path evaluationInput, Optional<ByteArrayOutputStream> evaluationResults) throws Exception;
    
    @Test
    public void test() throws Exception {
        Optional<ByteArrayOutputStream> os = Optional.empty();
        boolean shouldTestPass = true;
        try
        {
            Assert.assertEquals("Evaluation filepath not found: " + evaluationFilepath, Files.exists(evaluationFilepath), true);

            shouldTestPass = Files.exists(expectedResultPath);
            os = Optional.of(new ByteArrayOutputStream());
            this.run(this.evaluationFilepath, os);
            final JsonElement evaluated = new JsonParser().parse(os.get().toString("UTF-8"));
            final String evaluatedString = new GsonBuilder().create().toJson(evaluated);

            // If we got this far, there needs to be a corresponding file to verify against
            if (shouldTestPass)
            {
                Assert.assertEquals("Expected filepath not found: " + expectedResultPath, Files.exists(expectedResultPath), true);
                final String expectedContent = new String(Files.readAllBytes(expectedResultPath), "UTF-8");
                JSONAssert.assertEquals(evaluatedString, expectedContent, false);
            }

            // A test that isn't supposed to pass should have thrown an Exception by
            // now.  If we didn't, then the test failed.
            final String failMsg = String.format("False positive result for input: %s, expected: %s", evaluationFilepath, expectedResultPath);
            Assert.assertEquals(failMsg, true, shouldTestPass);
        }
        catch (AssertionError err)
        {
            final String expected = new String(os.get().toByteArray(), "UTF-8");
            final String errMsg = String.format("%s%nExpected:%n%s%n%s", evaluationFilepath, expected, err.getMessage());
            throw new AssertionError(errMsg);
        }
        catch (Exception ex)
        {
        	logger.error(ex.toString());
            if (shouldTestPass)
            {
                if (os.isPresent())
                {
                    final String expected = new String(os.get().toByteArray(), "UTF-8");
                    logger.error("Expected:\n{}", expected);
                }
                final String errMsg = String.format("Failed to validate definition: %s%nExpected: %s%n%s", evaluationFilepath, expectedResultPath, ex.getMessage());
                throw new RuntimeException(errMsg);
            }
        } 	
    }
}
