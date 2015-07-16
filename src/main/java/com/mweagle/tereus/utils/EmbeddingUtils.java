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
// DEALINGS IN THE SOFTWARE

package com.mweagle.tereus.utils;

import com.google.gson.*;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mweagle on 4/26/15.
 */
public class EmbeddingUtils implements IEngineBinding {

	final static Pattern RE_TRAILING_NEWLINE = Pattern.compile(".+\\n$");
    /**
     * http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/pseudo-parameter-reference.html
     */
    public static final String[] AWS_PSEUDO_PARAMS_LIST = new String[]{"AWS::AccountId", "AWS::NotificationARNs", "AWS::NoValue", "AWS::Region", "AWS::StackId", "AWS::StackName"};
    public static final Set<String> AWS_PSEUDO_PARAMS = new HashSet<String>(Arrays.asList(AWS_PSEUDO_PARAMS_LIST));
    private final static Pattern PATTERN_MUSTACHE = Pattern.compile("\\{{2}([^\\}]+)\\}{2}");


    private final Logger logger;

    @Override
    public String getBindingName() {
        return "EmbeddingUtilsImpl";
    }

    public EmbeddingUtils(Path templateRoot, ScriptEngine engine, Logger logger) {
        this.logger = logger;
    }

    public String Literal(String rawData) throws Exception {
        JsonArray jsonContent =  this.parseResource(rawData);
        JsonArray fnJoinContent = new JsonArray();
        fnJoinContent.add(new JsonPrimitive(""));
        fnJoinContent.add(jsonContent);
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("Fn::Join", fnJoinContent);
        final Gson serializer = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().create();
        final String stringified = serializer.toJson(jsonObject);
        return stringified;
    }

    protected JsonArray parseResource(final String resourceData) throws Exception {
        JsonArray parsedContent = new JsonArray();
        Arrays.stream(resourceData.split("\\r?\\n")).forEach(eachLine ->
                parsedContent.addAll(parseLine(eachLine)));
        // Get the last element of parsed content.  If it's a JsonPrimitive with some non-empty
        // content then remove the final newline delimiter
        final JsonElement finalElement = parsedContent.get(parsedContent.size()-1);
        if (finalElement instanceof JsonPrimitive) {
			JsonPrimitive finalPrimitive = (JsonPrimitive) finalElement;
			final String primitiveContent = finalPrimitive.getAsString();
			if (RE_TRAILING_NEWLINE.matcher(primitiveContent).matches())
			{
				final JsonPrimitive trimmedPrimitive = new JsonPrimitive(primitiveContent.substring(0, primitiveContent.length()-1));
				parsedContent.set(parsedContent.size()-1, trimmedPrimitive);
			}

		}
        return parsedContent;
    }

    protected JsonArray parseLine(final String input) throws IllegalArgumentException {
        Matcher match = EmbeddingUtils.PATTERN_MUSTACHE.matcher(input);
        JsonArray parsed = new JsonArray();
        int lastPos = 0;
        String slice = "";
        while (match.find()) {
            int matchStart = match.start();
            int matchEnd = match.end();
            if (matchStart > lastPos) {
                slice = input.substring(lastPos, matchStart);
                logger.debug("EmbeddingSlice 1: {{}}", slice);

                parsed.add(new JsonPrimitive(slice));
            }
            // The match includes the {{, }} chars.  We're going to strip those so that
            // the resulting string is a JSON object we can parse and turn into the appropriate
            // AWS magic
            slice = input.substring(matchStart+1, matchEnd-1);
            logger.debug("EmbeddingSlice 2: {{}}", slice);
            try
            {
                JsonObject awsExpression = new Gson().fromJson(slice, JsonObject.class);
                parsed.add(awsExpression);
            }
            catch (Exception ex)
            {
                this.logger.error("Failed to parse line for AWS expressions: {{}}", input);
                throw ex;
            }

            // Offset the iterator and continue matching
            lastPos = matchEnd;
        }
        if (lastPos <= input.length()) {
            slice = input.substring(lastPos);
            logger.debug("EmbeddingSlice 3: {{}}", slice);

            parsed.add(new JsonPrimitive(slice + "\n"));
        }
        return parsed;
    }
}
