package com.mweagle.tereus;

/**
 * Publicly significant constants.
 */
public class CONSTANTS {
    public final static String TEREUS_VERSION = "0.1.0";

    public final static String TEREUS_TAG_NAMESPACE = "com:tereus";

    public final static class ARGUMENT_JSON_KEYNAMES
    {
        /**
         * The keyname in the nested object that represents Parameter values
         * to provide to the CloudFormation template
         */
        public final static String PARAMETERS = "Parameters";
        /**
         * They keyname in the nested object that represents additional
         * tags to apply to all relevant CloudFormation resources
         */
        public final static String TAGS = "Tags";
    }
    /**
     * Reserved parameter names that are explicitly referenced in the default CloudFormation
     * Template and shoudl be provided on the command line.
     */
    public final static class PARAMETER_NAMES
    {
        /**
         * The S3 bucketname to use to host the template.  The template is uploaded to the
         * S3 bucket to allow for larger <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cloudformation-limits.html">template sizes</a>
         */
        public final static String S3_BUCKET_NAME = "BucketName";
        /**
         * Name of a pre-existing SSH key.  Only required if there is an AutoScalingGroup or
         * EC2 instance provisioned.
         */
        public final static String SSH_KEY_NAME = "KeyName";
        /**
         * Optional path to local Docker file created by `docker --save --output image.tar.gz`.  If this
         * parameter is provided and there is an ASG or EC2 provisioned, then the Docker image
         * will be bound to the instance
         */
        public final static String DOCKER_IMAGE_PATH =  "DockerImagePath";
        /**
         * Instance type to use.  Typically refers to the `AWSInstanceType2Arch`
         * Mappings entry.
         */
        public final static String INSTANCE_TYPE =  "InstanceType";
    }
}
