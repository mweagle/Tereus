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
        public final static String SSH_KEY_NAME = "SSHKeyName";
        /**
         * Instance type to use.  Typically refers to the `AWSInstanceType2Arch`
         * Mappings entry.
         */
        public final static String INSTANCE_TYPE =  "InstanceType";
    }
}
