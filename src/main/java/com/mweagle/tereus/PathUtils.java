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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * Created by mweagle on 4/26/15.
 */
public class PathUtils {
    private static final String[] PATH_SUFFIXES = {"", ".js", ".json", File.pathSeparator + "index.js"};

    public static Path resolvePath(Path root, String arg) throws FileNotFoundException {
        Optional<Path> matchingPath = Arrays.stream(PathUtils.PATH_SUFFIXES).
                map(suffix -> root.resolve(arg + suffix).normalize()).
                filter(eachPathTest -> Files.exists(eachPathTest)).
                findFirst();
        if (matchingPath.isPresent()) {
            return matchingPath.get();
        }
        throw new FileNotFoundException(root.resolve(arg).toString());
    }
}
