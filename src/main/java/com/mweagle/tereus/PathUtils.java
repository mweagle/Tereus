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
