package tereus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

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
    protected static final String EVALUATION_ROOT_DIRECTORY = "src/test/java/tereus/evaluation";
    @Parameter(0)
    public Path evaluationFilepath;
    @Parameter(1)
    public Path expectedFilepath;

    @Parameters
    public static List<Object[]> data() throws IOException {
        final List<Path> testDirectories = Files.list(Paths.get(EVALUATION_ROOT_DIRECTORY)).
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
