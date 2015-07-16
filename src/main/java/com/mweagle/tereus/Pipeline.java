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
package com.mweagle.tereus;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
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
    private static final String[] JS_FILES = {
    		"node_modules/underscore/underscore-min.js",
            "node_modules/immutable/dist/immutable.min.js",
    		"main/index.js",
            "main/CONSTANTS.js",
            "main/CloudFormationTemplate.js",
            /** AWS Helpers **/
            "main/aws/index.js",
            "main/aws/lambda.js",
            "main/aws/ec2.js"
            };
    
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
    	
    	// get the info
    	final AmazonIdentityManagementClient client = new AmazonIdentityManagementClient();	
    	final GetUserResult result = client.getUser();
        engine.put("UserInfoImpl", result);
    	
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
            	this.logger.error(ex);
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

        @Override
        public IEngineBinding apply(String className) {
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> ctor = null;
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
