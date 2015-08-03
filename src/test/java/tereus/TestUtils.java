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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

public class TestUtils
{
	public final static Path testRoot() 
	{
		final String cwd = new File("").getAbsolutePath();
		final Path testRootDirectory = Paths.get(cwd, "src", "test", "java",
				"tereus");
        Preconditions.checkArgument(TestUtils.isValidRootArgument(testRootDirectory), 
        							"Failed to resolve test root directory (eg: ~/Documents/Tereus/src/test/java/tereus)");
        return testRootDirectory;
	}
	
	public static List<Object[]> definitionAndResultPairs(final String subfolder) throws IOException
	{
		final List<Path> testDirectories = Files.list(Paths.get(TestUtils.testRoot().toString(), subfolder))
				.filter(Files::isDirectory).collect(Collectors.toList());
		List<Object[]> testPairs = new LinkedList<>();
		testDirectories.forEach(eachPath -> {
			try
			{
				final Path definitionDirectory = eachPath.resolve("definition");
				final Path expectedDirectory = eachPath.resolve("expected");
				try
				{
					Files.list(definitionDirectory).forEach(eachDefinitionPath -> {
						if (Files.isRegularFile(eachDefinitionPath))
						{
							final String filename = eachDefinitionPath.getFileName().toString();
							final String expectedFilename = String.format("%son", filename);
							testPairs.add(new Object[] { eachDefinitionPath.toAbsolutePath(),
									expectedDirectory.resolve(expectedFilename).toAbsolutePath() });
						}
					});
				} catch (Exception ex)
				{
					throw new RuntimeException(ex);
				}
			} catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		});
		return testPairs;
	}
	
    protected static boolean isValidRootArgument(Path argument)
    {
        return (Files.exists(argument) &&
                Files.isDirectory(argument));
    }
}
