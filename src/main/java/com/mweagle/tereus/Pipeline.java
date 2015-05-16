package com.mweagle.tereus;

import com.google.gson.Gson;
import com.mweagle.TereusInput;
import com.mweagle.tereus.utils.IEngineBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by mweagle on 4/25/15.
 */
public class Pipeline {
    private static final String BINDING_PACKAGE = "com.mweagle.tereus.utils";
    private static final String[] BINDING_CLASSES = {"CloudFormationTemplateUtils",
                                                    "EmbeddingUtils",
                                                    "FileUtils"};
    private static final String BINDING_RESOURCE_ROOT = "js/bindings";

    private static final String[] JS_FILES = {"underscore.js",
            "immutable-js-3.7.2/dist/immutable.min.js",
            "CONSTANTS.js",
            "CloudFormationTemplate.js"};

    /**
     * Get the input file
     * Evaluate the input file
     */
    public HashMap<String, Object> run(final TereusInput cfInput ) throws Exception {
        final Instant startTime = Instant.now();
        final String cfTemplate = new String(Files.readAllBytes(cfInput.stackDefinitionPath));

        cfInput.logger.debug("Creating ScriptEngine Context");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        // Globals
        this.bindGlobals(engine, cfInput);

        // Get all the utils and ensure that they implement IEngineBinding.  For each one
        // we'll bind them into the Javascript
        List<IEngineBinding> boundInstances = Arrays.stream(Pipeline.BINDING_CLASSES).
                map(new ClassNameBuilder()).
                map(new EngineBinding(cfInput, engine)).
                collect(Collectors.toList());

        // Add the custom JavaScript classes to the evaluation context
        Arrays.stream(JS_FILES).forEach(new JavaScriptInputStreamPublisher(engine, BINDING_RESOURCE_ROOT, cfInput.logger));
        cfInput.logger.debug("Evaluating template");

        // Run the template...
        engine.eval(cfTemplate);

        HashMap<String, Object> result = new HashMap<>();
        for (IEngineBinding eachBinding : boundInstances) {
            if (null != eachBinding.getEvaluationResult()) {
                result.putAll(eachBinding.getEvaluationResult());
            }
        }

        final Duration totalTime = Duration.between(startTime, Instant.now());
        cfInput.logger.info("Template evaluation duration: {} ms", totalTime.toMillis());
        return result;
    }

    private ScriptEngine bindGlobals(ScriptEngine engine, TereusInput cfInput) {

        Supplier<String> fnArgs = () -> {
            HashMap<String, Map<String, Object>> args = new HashMap<>();
            args.put("params", cfInput.params);
            args.put("tags", cfInput.tags);
            Gson gson = new Gson();
            return gson.toJson(args);
        };
        engine.put("ArgumentsImpl", fnArgs);

        // And the logger
        final Logger templateLogger  = LogManager.getLogger("com.mweagle.Tereus.TemplateEvaluation");
        engine.put("logger", templateLogger);

        return engine;
    }

    protected class ClassNameBuilder implements Function<String, String> {
        @Override
        public String apply(String s) {
            return String.join(".", Pipeline.BINDING_PACKAGE, s);
        }
    }

    protected abstract class JavaScriptPublisher implements Consumer<String>
    {
        final ScriptEngine engine;
        final Logger logger;

        public JavaScriptPublisher(ScriptEngine engine, Logger logger)
        {
            this.engine = engine;
            this.logger = logger;
        }
        void evaluate(Reader jsReader, String errorMessage)
        {
            boolean evaluated = false;
            try {
                this.engine.eval(jsReader);
                evaluated = true;
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
            finally
            {
                if (!evaluated)
                {
                    this.logger.error(errorMessage);
                }
            }
        }
    }
    protected class JavaScriptInputStreamPublisher extends Pipeline.JavaScriptPublisher
    {
        final String resourcesRoot;

        public JavaScriptInputStreamPublisher(ScriptEngine engine, final String resourcesRoot, final Logger logger)
        {
            super(engine, logger);
            this.resourcesRoot = resourcesRoot;
        };

        @Override
        public void accept(String resourceName) {
            final String resourcePath = String.format("%s/%s", this.resourcesRoot, resourceName);
            this.logger.debug("Requiring resource: " + resourcePath);
            final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            super.evaluate(reader, "Failed to load resource: " + resourcePath);
        }
    }

    protected class EngineBinding implements Function<String, IEngineBinding> {
        private final TereusInput tereusInput;
        private final ScriptEngine engine;
        private final Path templateRootPath;

        public EngineBinding(TereusInput input, ScriptEngine engine) {
            this.tereusInput = input;
            this.templateRootPath = input.stackDefinitionPath.getParent();
            this.engine = engine;
        }

        @SuppressWarnings("unchecked")
        @Override
        public IEngineBinding apply(String className) {
            try {
                Class clazz = Class.forName(className);
                Constructor ctor = null;
                IEngineBinding bound = null;
                try {
                    ctor = clazz.getConstructor(Path.class, ScriptEngine.class, Logger.class);
                    bound = (IEngineBinding) ctor.newInstance(this.templateRootPath, this.engine, tereusInput.logger);
                } catch (NoSuchMethodException ex) {
                    ctor = clazz.getConstructor();
                    bound = (IEngineBinding) ctor.newInstance();
                }
                engine.put(bound.getBindingName(), bound);
                return bound;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}