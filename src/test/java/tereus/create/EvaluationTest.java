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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.commands.CreateCommand;
import com.mweagle.tereus.input.TereusInput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by mweagle on 5/13/15.
 */
public abstract class EvaluationTest {

    final protected Logger logger = LogManager.getLogger(EvaluationTest.class.getName());

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    protected void verifyEvaluation(final TereusInput input, final Path expectedResultPath) throws Exception
    {
        CreateCommand tereus = new CreateCommand();

        Optional<ByteArrayOutputStream> os = Optional.empty();
        boolean shouldTestPass = true;
        try
        {
            shouldTestPass = Files.exists(expectedResultPath);
            os = Optional.of(new ByteArrayOutputStream());
            tereus.create(input, os);
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
            final String failMsg = String.format("False positive result for input: %s, expected: %s", input.stackDefinitionPath, expectedResultPath);
            Assert.assertEquals(failMsg, true, shouldTestPass);
        }
        catch (AssertionError err)
        {
            final String expected = new String(os.get().toByteArray(), "UTF-8");
            final String errMsg = String.format("%s%nExpected:%n%s%n%s", input.stackDefinitionPath, expected, err.getMessage());
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
                final String errMsg = String.format("Failed to validate definition: %s%nExpected: %s%n%s", input.stackDefinitionPath, expectedResultPath, ex.getMessage());
                throw new RuntimeException(errMsg);
            }
        }
    }

    public void verifyEvaluation(final Path evaluationFilepath, final Path expectedResultPath) throws Exception
    {
        Assert.assertEquals("Evaluation filepath not found: " + evaluationFilepath, Files.exists(evaluationFilepath), true);

        Map<String, Object> params = new HashMap<>();
        params.put(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, "testBucket");

        final TereusInput input = new TereusInput(null, evaluationFilepath.toString(), null, params, new HashMap<String, Object>(), true);
        this.verifyEvaluation(input, expectedResultPath);
    }
}