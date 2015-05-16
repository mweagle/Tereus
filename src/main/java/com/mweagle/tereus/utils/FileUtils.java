package com.mweagle.tereus.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by mweagle on 4/29/15.
 */
public class FileUtils implements IEngineBinding {

    private final Path templateRoot;
    private final Logger logger;
    public FileUtils(Path templateRoot, ScriptEngine engine, Logger logger) {
        this.templateRoot = templateRoot;
        this.logger = logger;
        this.logger.debug("Resource root directory: {}", this.templateRoot.toAbsolutePath());
    }

    @Override
    public String getBindingName() {
        return "FileUtils";
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
