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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Charsets;

public abstract class NashornEvaluator implements INashornEvaluatorContext
{
	protected void publishGlobals(ScriptEngine engine)
	{
		// NOP
	}

	protected Stream<String> javaClassnames()
	{
		return Stream.empty();
	}

	protected Stream<String> javascriptResources()
	{
		return Stream.empty();
	}

	public HashMap<String, Object> run(final Path jsFilePath, final Logger logger) throws Exception
	{
		final Instant startTime = Instant.now();
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

		// Globals
		this.publishGlobals(engine);

		// Get all the JavaClasses that should be registered and map them in
		// there...
		List<INashornEvaluationAccumulator> boundInstances = this.javaClassnames().map(new EngineBinding(this, engine))
				.collect(Collectors.toList());

		// Add the custom JavaScript classes to the evaluation context
		this.javascriptResources().forEach(new JavaScriptInputStreamPublisher(engine, logger));
		logger.debug("Evaluating");

		// Evaluate the template
		try
		{
	        final String jsFile = new String(Files.readAllBytes(jsFilePath), Charsets.UTF_8);
			engine.eval(jsFile);
		} catch (Exception ex)
		{
			final String msg = String.format("Failed to evaluate: %s%n%s: %s", jsFilePath, ex.getClass().getName(),
					ex.getMessage());
			throw new Exception(msg, ex.getCause());
		}

		HashMap<String, Object> result = new HashMap<>();
		for (INashornEvaluationAccumulator eachBinding : boundInstances)
		{
			if (null != eachBinding.getAccumulationResult())
			{
				result.putAll(eachBinding.getAccumulationResult());
			}
		}
		final Duration totalTime = Duration.between(startTime, Instant.now());
		logger.info("Template evaluation duration: {} ms", totalTime.toMillis());
		return result;
	}

	protected abstract class JavaScriptPublisher implements Consumer<String>
	{
		final ScriptEngine engine;
		final Logger logger;

		public JavaScriptPublisher(ScriptEngine engine, Logger logger) {
			this.engine = engine;
			this.logger = logger;
		}

		void evaluate(Reader jsReader, String errorMessage)
		{
			boolean evaluated = false;
			try
			{
				this.engine.eval(jsReader);
				evaluated = true;
			} catch (Exception ex)
			{
				this.logger.error(ex);
				throw new RuntimeException(ex);
			} finally
			{
				if (!evaluated)
				{
					this.logger.error(errorMessage);
				}
			}
		}
	}

	protected class JavaScriptInputStreamPublisher extends NashornEvaluator.JavaScriptPublisher
	{
		public JavaScriptInputStreamPublisher(ScriptEngine engine, final Logger logger) {
			super(engine, logger);
		};

		@Override
		public void accept(final String resourcePath)
		{
			this.logger.debug("Loading resource: " + resourcePath);
			final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
			if (null == in)
			{
				throw new RuntimeException("Failed to load JS resource: " + resourcePath);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
			super.evaluate(reader, "Failed to load resource: " + resourcePath);
		}
	}

	protected static class EngineBinding implements Function<String, INashornEvaluationAccumulator>
	{
		private final INashornEvaluatorContext evaluationContext;
		private final ScriptEngine engine;

		public EngineBinding(INashornEvaluatorContext context, ScriptEngine engine) {
			this.evaluationContext = context;
			this.engine = engine;
		}

		@Override
		public INashornEvaluationAccumulator apply(String className)
		{
			try
			{
				Class<?> clazz = Class.forName(className);
				Constructor<?> ctor = null;
				INashornEvaluationAccumulator bound = null;
				try
				{
					ctor = clazz.getConstructor(INashornEvaluatorContext.class);
					bound = (INashornEvaluationAccumulator) ctor.newInstance(this.evaluationContext);
				} catch (NoSuchMethodException ex)
				{
					ctor = clazz.getConstructor();
					bound = (INashornEvaluationAccumulator) ctor.newInstance();
				}
				engine.put(bound.getAccumulatorName(), bound);
				return bound;
			} catch (RuntimeException e)
			{
				throw e;
			} catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
