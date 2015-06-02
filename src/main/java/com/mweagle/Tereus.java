package com.mweagle;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mweagle.tereus.CONSTANTS;
import com.mweagle.tereus.Pipeline;
import com.mweagle.tereus.aws.CloudFormation;
import com.mweagle.tereus.aws.S3Resource;
import io.airlift.airline.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;

import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Command(name = "Tereus", description = "Evaluates a CloudFormation template expressed as a function")
public class Tereus {
    @Inject
    public HelpOption helpOption;

    @Option(name = {"-t", "--template"}, description = "[REQUIRED] Path to CloudFormation definition")
    public String stackTemplatePath;

    @Option(name = {"-a", "--arguments"}, description = "Path to JSON file including \"Parameters\" & \"Tags\" values")
    public String jsonParamAndTagsPath;

    @Option(name = {"-b", "--bucket"}, description = "[REQUIRED] S3 Bucketname to host template content")
    public String s3BucketName;

    @Option(name = {"-d", "--dockerFile"}, description = "DockerFile path")
    public String dockerFilePath;

    @Option(name = {"-r", "--region"}, description="AWS Region (default=us-east-1)")
    public String region = "us-east-1";

    @Option(name = {"-s", "--stack"}, description="Optional Stack Name to use.  If empty, {basename+SHA256(templateData)} will be provided")
    public String stackName;

    @Option(name = {"-o", "--output"}, description="Optional file to which evaluated template will be saved")
    public String outputFilePath;

    @Option(name = {"-n", "--noop"}, description = "Dry run - stack will NOT be created (default=true)")
    public String noop = "true";

    /* The accumulated TereusInput data */
    protected TereusInput tereusInput;

    @SuppressWarnings("unchecked")
    public static void main(String... args) {
        Tereus tereus = SingleCommand.singleCommand(Tereus.class).parse(args);
        int exitCode = 0;

        if (tereus.helpOption.showHelpIfRequested())
        {
            return;
        }
        try
        {
            final String argumentJSON = (null != tereus.jsonParamAndTagsPath) ?
                    new String(Files.readAllBytes(Paths.get(tereus.jsonParamAndTagsPath)), "UTF-8") :
                    null;

            Map<String, Object> jsonJavaRootObject = (null != argumentJSON) ?
                    new Gson().fromJson(argumentJSON, Map.class) :
                    Collections.emptyMap();
            Map<String, Object> parameters = (Map<String, Object>)jsonJavaRootObject.getOrDefault(CONSTANTS.ARGUMENT_JSON_KEYNAMES.PARAMETERS, new HashMap<>());
            final String jsonS3BucketName = ((String)parameters.getOrDefault(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, "")).trim();
            final String cliS3BucketName = (null == tereus.s3BucketName) ? "" : tereus.s3BucketName.trim();

            if (!jsonS3BucketName.isEmpty() && !cliS3BucketName.isEmpty())
            {
                final String msg = String.format("S3 bucketname defined in both %s and via command line argument",
                                                tereus.jsonParamAndTagsPath);
                throw new IllegalArgumentException(msg);
            }
            else if (!cliS3BucketName.isEmpty())
            {
                parameters.put(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME, cliS3BucketName);
            }


            Map<String, Object> tags = (Map<String, Object>)jsonJavaRootObject.getOrDefault(CONSTANTS.ARGUMENT_JSON_KEYNAMES.TAGS,
                    Collections.emptyMap());

            TereusInput tereusInput =  new TereusInput(tereus.stackName,
                                                            tereus.stackTemplatePath,
                                                            tereus.dockerFilePath,
                                                            tereus.region,
                                                            parameters,
                                                            tags,
                                                            Boolean.parseBoolean(tereus.noop));
            Optional<OutputStream> osSink = Optional.empty();
            try
            {
                if (null != tereus.outputFilePath)
                {
                    final Path outputPath = Paths.get(tereus.outputFilePath);
                    osSink = Optional.of(new FileOutputStream(outputPath.toFile()));
                }
                tereus.run(tereusInput, osSink);
            }
            catch (Exception ex)
            {
                LogManager.getLogger().error(ex);
                exitCode = 2;
            }
            finally
            {
              if (osSink.isPresent())
              {
                  osSink.get().close();
              }
            }
        }
        catch (Exception ex)
        {
            LogManager.getLogger().error(ex);
            Help.help(tereus.helpOption.commandMetadata);
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    public void run(final TereusInput tereusInput, Optional<? extends  OutputStream> osSinkTemplate) throws Exception
    {
        java.security.Security.setProperty("networkaddress.cache.ttl", "30");

        // Validate the arguments
        Pipeline pipeline = new Pipeline();

        // Expand the template
        Map<String, Object> evaluationResult = pipeline.run(tereusInput);
        final Optional<Object> templateData =  Optional.ofNullable(evaluationResult.get("Template"));
        final Optional<Object> prepopulatedTemplate =  Optional.ofNullable(evaluationResult.get("ParameterizedTemplate"));

        if (templateData.isPresent())
        {
            // It's possible there's a command line option that overrides the
            // stack name
            if (null == this.stackName)
            {
                final String stackBaseName = (String)(evaluationResult.get("StackName"));
                final String templateString = new GsonBuilder().create().toJson(templateData.get());
                this.stackName = String.format("%s-%s", stackBaseName, DigestUtils.sha256Hex(templateString));
            }
        }

        /**
         * To put this into JSON:
         * This is the result, but
         * Gson gson = new GsonBuilder().setPrettyPrinting().create();
         * return gson.toJson(result);
         */
        // Upload the two templates to s3...

        // Get the template

        // Upload the parameterized template to S3, validate it, and cleanup
        validateTemplate(tereusInput, new GsonBuilder().disableHtmlEscaping().create().toJson(prepopulatedTemplate.get()));

        // Create the stack
        createStack(tereusInput, (JsonElement) templateData.get(), !osSinkTemplate.isPresent());

        if (osSinkTemplate.isPresent())
        {
            final String formattedTemplate = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(templateData.get());
            osSinkTemplate.get().write(formattedTemplate.getBytes(Charset.forName("UTF-8")));
        }
    }
    protected List<Parameter> toParameterList(final Map<String, Object> values)
    {
        return values.entrySet().stream().map(eachEntry -> {
            Parameter awsParam = new Parameter();
            awsParam.setParameterKey(eachEntry.getKey());
            awsParam.setParameterValue(eachEntry.getValue().toString());
            return awsParam;
        }).collect(Collectors.toList());
    }

    protected List<Tag> toTagList(final Map<String, Object> values)
    {
        return values.entrySet().stream().map(eachEntry -> {
            Tag awsTag = new Tag();
            awsTag.setKey(eachEntry.getKey());
            awsTag.setValue(eachEntry.getValue().toString());
            return awsTag;
        }).collect(Collectors.toList());
    }

    protected void createStack(TereusInput tereusInput, JsonElement templateData, boolean logTemplate)
    {
        if (tereusInput.dryRun)
        {
            tereusInput.logger.info("Dry run requested (-n/--noop). Stack creation bypassed.");
            if (logTemplate)
            {
                final String formattedTemplate = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(templateData);
                tereusInput.logger.info("Stack Template:\n {}", formattedTemplate);
            }
        }
        else
        {
            final String bucketName = tereusInput.params.get(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME).toString();
            // Upload the template
            final String templateContent = new GsonBuilder().create().toJson(templateData);
            try ( S3Resource resource = new S3Resource(bucketName, "template", templateContent))
            {
                resource.upload();
                final EstimateTemplateCostRequest costRequest = new EstimateTemplateCostRequest();
                costRequest.setParameters(toParameterList(tereusInput.params));
                costRequest.setTemplateURL(resource.getResourceURL().get());
                final AmazonCloudFormationClient awsClient = new AmazonCloudFormationClient(tereusInput.awsCredentials);
                awsClient.setRegion(tereusInput.awsRegion);
                final EstimateTemplateCostResult costResult = awsClient.estimateTemplateCost(costRequest);
                tereusInput.logger.info("Cost Estimator: {}", costResult.getUrl());

                // Go ahead and create the stack.
                final CreateStackRequest request = new CreateStackRequest();
                request.setStackName(stackName);
                request.setTemplateURL(resource.getResourceURL().get());
                request.setParameters(toParameterList(tereusInput.params));
                request.setTags(toTagList(tereusInput.tags));
                tereusInput.logger.debug("Creating stack: {}", stackName);
                tereusInput.logger.debug("Stack params: {}", request.getParameters());
                tereusInput.logger.debug("Stack tags: {}", request.getTags());
                final Optional<DescribeStacksResult> result = new CloudFormation().createStack(request, tereusInput.awsRegion, tereusInput.logger);
                if (result.isPresent())
                {
                    tereusInput.logger.info("Stack successfully created");
                    tereusInput.logger.info(result.get().toString());
                    resource.setReleased(true);
                }
            }
        }
    }

    protected void validateTemplate(TereusInput tereusInput, String parameterizedTemplate)  {
        if (tereusInput.dryRun)
        {
            tereusInput.logger.info("Dry run requested (-n/--noop). Stack validation bypassed.");
        }
        else
        {
            tereusInput.logger.info("Validating template with AWS");
            final String bucketName = tereusInput.params.get(CONSTANTS.PARAMETER_NAMES.S3_BUCKET_NAME).toString();
            try ( S3Resource resource = new S3Resource(bucketName, "prepopulated", parameterizedTemplate))
            {
                Optional<String> templateURL = resource.upload();
                final ValidateTemplateRequest validationRequest = new ValidateTemplateRequest();
                validationRequest.setTemplateURL(templateURL.get());
                final AmazonCloudFormationClient awsClient = new AmazonCloudFormationClient(tereusInput.awsCredentials);
                awsClient.setRegion(tereusInput.awsRegion);
                final ValidateTemplateResult validationResult = awsClient.validateTemplate(validationRequest);
                tereusInput.logger.debug("Stack template validation results:");
                tereusInput.logger.debug(validationResult.toString());
            }
        }
    }
}
