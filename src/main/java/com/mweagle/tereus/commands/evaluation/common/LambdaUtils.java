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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mweagle.tereus.INashornEvaluationAccumulator;
import com.mweagle.tereus.INashornEvaluatorContext;
import com.mweagle.tereus.aws.S3Resource;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("unused")
public class LambdaUtils implements INashornEvaluationAccumulator {

    private Path templateRoot;
    private boolean dryRun;
    private Logger logger;
    public LambdaUtils()
    {

    }

    @Override
    public String bind(INashornEvaluatorContext context)
    {
        this.templateRoot = context.getEvaluationSource().getParent();
        this.dryRun = context.isDryRun();
        this.logger = context.getLogger();
        return "LambdaUtilsImpl";
    }

    public String createFunction(final String logicalResourceName,
                                    final String lambdaSourceRoot,
                                    final String bucketName,
                                    final String s3KeyName) throws IOException, InterruptedException, NoSuchAlgorithmException
    {

        // Build it, zip it, and upload it.  Return:
        /*
            {
              "S3Bucket" : String,
              "S3Key" : String,
              "S3ObjectVersion" : "TODO - not yet implemented"
            }
        */
        this.logger.info("Looking for source {} relative to {}", lambdaSourceRoot, templateRoot);
        final String lambdaDir = this.templateRoot.resolve(lambdaSourceRoot).normalize().toAbsolutePath().toString();
        final Path lambdaPath = Paths.get(lambdaDir);

        // Build command?
        final Optional<String> buildCommand = lambdaBuildCommand(lambdaDir);
        if (buildCommand.isPresent())
        {
            this.logger.info("{} Lambda source: {}", buildCommand.get(), lambdaDir);
            try
            {
                Runtime rt = Runtime.getRuntime();
                Process pr = rt.exec(buildCommand.get(), null, new File(lambdaDir));
                this.logger.info("Waiting for `{}` to complete", buildCommand.get());

                final int buildExitCode = pr.waitFor();
                if (0 != buildExitCode)
                {
                    logger.error("Failed to `{}`: {}", buildCommand.get(), buildExitCode);
                    throw new IOException(buildCommand.get() + " failed for: " + lambdaDir);
                }
            }
            catch (Exception ex)
            {
                final String processPath = System.getenv("PATH");
                this.logger.error("`{}` failed. Confirm that PATH contains the required executable.", buildCommand.get());
                this.logger.error("$PATH: {}", processPath);
                throw ex;
            }
        }
        else
        {
            this.logger.debug("No additional Lambda build file detected");
        }
        
        Path lambdaSource = null;
        boolean cleanupLambdaSource = false;
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        try
        {
            final BiPredicate<Path, java.nio.file.attribute.BasicFileAttributes> matcher =  (path, fileAttrs) -> {
                final String fileExtension = com.google.common.io.Files.getFileExtension(path.toString());
                return (fileExtension.toLowerCase().compareTo("jar") == 0);
            };

            // Find/compress the Lambda source
            // If there is a JAR file in the source root, then use that for the upload
            List<Path> jarFiles = Files.find(lambdaPath, 1, matcher).collect(Collectors.toList());

            if (!jarFiles.isEmpty())
            {
                Preconditions.checkArgument(jarFiles.size() == 1, "More than 1 JAR file detected in directory: {}", lambdaDir);
                lambdaSource = jarFiles.get(0);
                md.update(Files.readAllBytes(lambdaSource));
            }
            else
            {
                lambdaSource = Files.createTempFile("lambda-", ".zip");
                this.logger.info("Zipping lambda source code: {}", lambdaSource.toString());
                final FileOutputStream os = new FileOutputStream(lambdaSource.toFile());
                final ZipOutputStream zipOS = new ZipOutputStream(os);
                createStableZip(zipOS, lambdaPath, lambdaPath, md);
                zipOS.close();
                this.logger.info("Compressed filesize: {} bytes", lambdaSource.toFile().length());
                cleanupLambdaSource = true;
            }

            // Upload it
            final String sourceHash = Hex.encodeHexString(md.digest());
            this.logger.info("Lambda source hash: {}", sourceHash);
            if (!s3KeyName.isEmpty())
            {
                this.logger.warn("User supplied S3 keyname overrides content-addressable name. Automatic updates disabled.");
            }
            final String keyName = !s3KeyName.isEmpty() ?
                                    s3KeyName :
                                    String.format("%s-lambda-%s.%s",
                                                logicalResourceName,
                                                sourceHash,
                                                com.google.common.io.Files.getFileExtension(lambdaSource.toString()));
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("S3Bucket", new JsonPrimitive(bucketName));
            jsonObject.add("S3Key", new JsonPrimitive(keyName));

            // Upload it to s3...
            final FileInputStream fis = new FileInputStream(lambdaSource.toFile());
            try ( S3Resource resource = new S3Resource(bucketName, keyName, fis, Optional.of(lambdaSource.toFile().length())))
            {
                this.logger.info("Source payload S3 URL: {}", resource.getS3Path());

                if (resource.exists())
                {
                    this.logger.info("Source {} already uploaded to S3", keyName);
                }
                else if (!this.dryRun)
                {
                    Optional<String> result = resource.upload();
                    this.logger.info("Uploaded Lambda source to: {}", result.get());
                    resource.setReleased(true);
                }
                else
                {
                    this.logger.info("Dry run requested (-n/--noop). Lambda payload upload bypassed.");
                }
            }
            final Gson serializer = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().create();
            return serializer.toJson(jsonObject);
        }
        finally
        {
            if (cleanupLambdaSource)
            {
                this.logger.debug("Deleting temporary file: {}", lambdaSource.toString());
                Files.deleteIfExists(lambdaSource);
            }
        }
    }

    protected void createStableZip(ZipOutputStream zipOS, Path parentDirectory, Path archiveRoot, MessageDigest md) throws IOException
    {
        // Sort & zip files
        final List<Path> childDirectories = new ArrayList<>();
        final List<Path> childFiles = new ArrayList<>();
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(parentDirectory);
        for (Path eachChild : dirStream)
        {
            if (Files.isDirectory(eachChild))
            {
                childDirectories.add(eachChild);
            }
            else
            {
                childFiles.add(eachChild);
            }
        }
        final int archiveRootLength = archiveRoot.toAbsolutePath().toString().length()+1;
        childFiles.stream().sorted().forEach(eachPath -> {
            final String zeName = eachPath.toAbsolutePath().toString().substring(archiveRootLength);
            try {
                final ZipEntry ze = new ZipEntry(zeName);
                zipOS.putNextEntry(ze);
                Files.copy(eachPath, zipOS);
                md.update(Files.readAllBytes(eachPath));
                zipOS.closeEntry();
            } catch (IOException ex) {
                throw new RuntimeException(ex.getMessage());
            }
        });

        childDirectories.stream().sorted().forEach(eachPath -> {
            try {
                createStableZip(zipOS, eachPath, archiveRoot, md);
            } catch (IOException ex) {
                throw new RuntimeException(ex.getMessage());
            }
        });
    }


    protected Optional<String> lambdaBuildCommand(final String lambdaRootDir)
    {
        final Map<String, String> builderMap = new ImmutableMap.Builder<String, String>()
                                                .put("package.json", "npm install")
                                                .put("build.xml", "ant")
                                                .put("build.gradle", "gradle build")
                                                .put("pom.xml","mvn clean deploy")
                                                .build();

        Optional<Map.Entry<String, String>> commandPair = builderMap.entrySet().stream().filter(eachPair ->
            Files.exists(Paths.get(lambdaRootDir, eachPair.getKey()))
        ).findFirst();
        return Optional.ofNullable(commandPair.isPresent() ? commandPair.get().getValue() : null);
    }
}