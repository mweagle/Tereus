package tereus;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mweagle.TereusInput;
import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.commands.CreateCommand;

/**
 * Created by mweagle on 5/12/15.
 */
@RunWith(org.junit.runners.JUnit4.class)
public class CreationTest  {
    final protected Logger logger = LogManager.getLogger(CreationTest.class.getName());

	private static String TEST_FILES_ROOT = System.getenv("JUNIT_TEST_ROOT");

	private static Path lambdaDefinitionPath()
	{				
		return Paths.get(CreationTest.TEST_FILES_ROOT, 
						 "evaluation",
						 "aws_samples",
						 "definition",
						 "Lambda.js").toAbsolutePath();
	};
	
	
    @Test
    public void test() throws Exception {
    	if (System.getenv().containsKey("JUNIT_CREATE_STACK"))
    	{
        	// Create the Lambda stack
            Map<String, Object> params = new HashMap<>();
            params.put(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, "weagle");

            final TereusInput input = new TereusInput(null, CreationTest.lambdaDefinitionPath().toString(), "us-west-2", params, new HashMap<String, Object>(), false);

            Optional<ByteArrayOutputStream> os = Optional.of(new ByteArrayOutputStream());
            CreateCommand tereus = new CreateCommand();
            tereus.create(input, os);
            final JsonElement evaluated = new JsonParser().parse(os.get().toString("UTF-8"));
            final String evaluatedString = new GsonBuilder().create().toJson(evaluated);
    		logger.info(evaluatedString);
    	}
    	else
    	{
    		logger.warn("Creation test bypassed.  Set env.JUNIT_CREATE_STACK to enable");
    	}
    }
}
