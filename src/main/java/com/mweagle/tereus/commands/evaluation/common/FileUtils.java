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
package com.mweagle.tereus.commands.evaluation.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.codec.digest.DigestUtils;

import com.mweagle.tereus.INashornEvaluationAccumulator;
import com.mweagle.tereus.INashornEvaluatorContext;

/**
 * Created by mweagle on 4/29/15.
 */
public class FileUtils implements INashornEvaluationAccumulator {

    private final Path templateRoot;

    public FileUtils(INashornEvaluatorContext context)
    {
        this.templateRoot = context.getEvaluationSource().getParent();
        if (null != context.getLogger())
        {
            context.getLogger().debug("Resource root directory: {}", this.templateRoot.toAbsolutePath());
        }
    }

    @Override
    public String getAccumulatorName() {
        return "FileUtilsImpl";
    }

    public String fileHash(String pathArg) throws Exception
    {
        final Path file = this.templateRoot.resolve(pathArg).normalize();
        try (InputStream is = Files.newInputStream(file))
        {
            return DigestUtils.sha256Hex(is);
        }
    }
    public String resolvedPath(String pathArg) throws Exception
    {
        return this.templateRoot.resolve(pathArg).normalize().toAbsolutePath().toString();
    }
    public String fileContents(String pathArg) throws Exception
    {
        try {
            Path resolved = this.templateRoot.resolve(pathArg).normalize();
            return new String(Files.readAllBytes(resolved), "UTF-8");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
