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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.commands.CreateCommand;
import com.mweagle.tereus.input.TereusInput;

import tereus.TestUtils;

/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(Parameterized.class)
public class TereusCreationTests extends tereus.EvaluationTest {
    @Parameters
    public static List<Object[]> data() throws IOException {
        return TestUtils.definitionAndResultPairs("create");
    }
	
    @Override
    protected  void run(Path evaluationInput, Optional<ByteArrayOutputStream> evaluationResults) throws Exception
    {
        Map<String, Object> params = new HashMap<>();
        params.put(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, "testBucket");

        final TereusInput input = new TereusInput(null, evaluationFilepath.toString(), null, params, new HashMap<String, Object>(), true);
        super.logger.info("Creation test: {}", evaluationFilepath.toString());
        new CreateCommand().create(input, evaluationResults);
    }
}
	