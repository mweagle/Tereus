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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import tereus.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(Parameterized.class)
public class TereusEvaluationTests extends EvaluationTest {
    @Parameter(0)
    public Path evaluationFilepath;
    @Parameter(1)
    public Path expectedFilepath;

    @Parameters
    public static List<Object[]> data() throws IOException {
        final List<Path> testDirectories = Files.list(Paths.get(TestUtils.testRoot().toString(), "create")).
                                                filter(Files::isDirectory).
                                                collect(Collectors.toList());
        List<Object[]> testPairs = new LinkedList<>();
        testDirectories.forEach(eachPath ->
        {
            try {
                final Path definitionDirectory = eachPath.resolve("definition");
                final Path expectedDirectory = eachPath.resolve("expected");
                try {
                    Files.list(definitionDirectory).forEach(eachDefinitionPath -> {
                        if (Files.isRegularFile(eachDefinitionPath))
                        {
                            final String filename = eachDefinitionPath.getFileName().toString();
                            final String expectedFilename = String.format("%son", filename);
                            testPairs.add(new Object[]{eachDefinitionPath.toAbsolutePath(),
                                    expectedDirectory.resolve(expectedFilename).toAbsolutePath()
                            });
                        }
                    });
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        return testPairs;
    }

    @Test
    public void test() throws Exception {
        super.verifyEvaluation(this.evaluationFilepath, this.expectedFilepath);
    }
}
