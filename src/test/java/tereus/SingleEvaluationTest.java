package tereus;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(org.junit.runners.JUnit4.class)
public class SingleEvaluationTest extends EvaluationTest {

    @Test
    public void test() throws Exception {
    	final Path evaluationFilepath = Paths.get("/Users/mweagle/Documents/GitHub/Tereus/src/test/java/tereus/evaluation/embedding/definition/file.js");
    	final Path expectedFilepath = Paths.get("/Users/mweagle/Documents/GitHub/Tereus/src/test/java/tereus/evaluation/embedding/expected/file.json");
        super.verifyEvaluation(evaluationFilepath, expectedFilepath);
    }
}
