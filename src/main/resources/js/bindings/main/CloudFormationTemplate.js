/* global logger,__templateTunnel,_,PARAMS,TAGS,CONSTANTS,FileUtilsImpl,EmbeddingUtilsImpl,LambdaUtilsImpl */
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

var CfnInitUserData = function (logicalResourceName) {
    var initializationLine = [];
    initializationLine.push('/opt/aws/bin/cfn-init -v');
    initializationLine.push('--stack {{ \"Ref\" : \"AWS::StackName\" }}');
    initializationLine.push('--resource <%= logicalResourceName %>');
    initializationLine.push('--configsets <%= configsetName %>');
    initializationLine.push('--region {{ \"Ref\" : \"AWS::Region\" }}');


    var data = ['#!/bin/bash -xe',
        'yum update -y aws-cfn-bootstrap',
        initializationLine.join(' ')
    ].join('\n');
    data = _.template(data)({
        logicalResourceName: logicalResourceName,
        configsetName: CONSTANTS.CLOUDINIT.DEFAULT_CONFIGSET_KEYNAME
    });
    return Embed.Literal(data);
};

/**
 * The global CloudFormationTemplate function responsible
 * for expanding the inline stack definition.
 *
 * @param {string} stackName - Name of the Stack
 */
function CloudFormationTemplate(stackName) {
    __templateTunnel.stackName = stackName;

    var __tagResource = function (resourceDefinition) {
        var taggable = ['AWS::AutoScaling::AutoScalingGroup',
            'AWS::EC2::CustomerGateway',
            'AWS::EC2::Instance',
            'AWS::EC2::InternetGateway',
            'AWS::EC2::NetworkAcl',
            'AWS::EC2::NetworkInterface',
            'AWS::EC2::RouteTable',
            'AWS::EC2::SecurityGroup',
            'AWS::EC2::Subnet',
            'AWS::EC2::Volume',
            'AWS::EC2::VPC',
            'AWS::EC2::VPCPeeringConnection',
            'AWS::EC2::VPNConnection',
            'AWS::EC2::VPNGateway',
            'AWS::ElasticLoadBalancing::LoadBalancer',
            'AWS::RDS::DBInstance',
            'AWS::RDS::DBParameterGroup',
            'AWS::RDS::DBSubnetGroup',
            'AWS::RDS::DBSecurityGroup',
            'AWS::S3::Bucket'
        ];

        var resTags = null;

        if (_.contains(taggable, resourceDefinition.Type)) {
            var extraTags = {};
            if ('AWS::AutoScaling::AutoScalingGroup' === resourceDefinition.Type) {
                extraTags = {
                    'PropagateAtLaunch': true
                };
            }

            resTags = _.map(TAGS.toObject(),
                function (eachTagValue, eachTagName) {
                    return _.extend({},
                        extraTags, {
                            'Key': eachTagName,
                            'Value': eachTagValue
                        });
                });
        }
        if (resTags) {
            resourceDefinition.Properties.Tags = _.extend(resTags,
                resourceDefinition.Properties.Tags || {});
        }
    };
    var __lambdaFunction = function(accumulator, resourceDefinition, logicalResourceName)
    {
        var lambdaTarget = ['AWS::Lambda::Function'];
        if (_.contains(lambdaTarget, resourceDefinition.Type) &&
            resourceDefinition.Properties)
        {
            var source = resourceDefinition.Properties.Code;
        	logger.info('Handling lamdbda: ' + logicalResourceName + ', source: ' + source);

            if (_.isString(source))
            {
                var bucket = PARAMS.get(CONSTANTS.PARAMETERS.KEYNAMES.BUCKET_NAME);

                accumulator[source] = accumulator[source] ||
                                        JSON.parse(LambdaUtilsImpl.createFunction(source, bucket, resourceDefinition.Properties.S3Key || ''));
                resourceDefinition.Properties.Code = accumulator[source];
                // Make sure there's no S3Key at the root...
                resourceDefinition.Properties.S3Key = undefined;
            }
        }
    };
    var __cloudInitInstances = function (resourceDefinition, logicalResourceName) {

        var cloudInitTarget = ['AWS::AutoScaling::AutoScalingGroup',
            'AWS::EC2::Instance'
        ];
        if (_.contains(cloudInitTarget, resourceDefinition.Type)) {
            var metadata = resourceDefinition.Metadata || {};
            var init = metadata['AWS::CloudFormation::Init'] || null;
            if (init) {
                // Ensure that the Userdata Cfn-init action is hooked up...
                var properties = resourceDefinition.Properties || {};
                if (_.isEmpty(properties.UserData)) {
                    properties.UserData = CfnInitUserData(logicalResourceName);
                }
                resourceDefinition.Properties = properties;
            }
        }
    };
    var __parameterizedTemplate = function (definition) {
        var params = definition.Parameters || {};
        _.each(params, function (eachValue, eachKey) {
            var paramValue = PARAMS.get(eachKey) || eachValue.Default || undefined;
            eachValue.Default = paramValue;
        });
        return definition;
    };

    return function (definition) {
        /*
         Apply the definition transformations to include:
         1. Docker parameter settings
         2. CloudFormation tags are passed to any eligible resources
         3. If there is a Docker image, add the cfn-init logic to spawn the downloaded image
         */
        definition.AWSTemplateFormatVersion = CONSTANTS.CLOUD_FORMATION.TEMPLATE_VERSION;

        ////////////////////////////////////////////////////////////////////////
        // BEGIN - Template resolution
        // Scan the resources and insert tags for anything that might have a tag
        _.each(definition.Resources, __tagResource.bind(this));

        // CloudInit bindings
        _.each(definition.Resources, __cloudInitInstances.bind(this));

        // Lambda uploads
        var logicalResources = Object.keys(definition.Resources);
        var self = this;
        _.reduce(logicalResources, 
        		function (memo, eachLogicalResource)
        		{
        			__lambdaFunction.call(self, memo, definition.Resources[eachLogicalResource], eachLogicalResource);
        			return memo;
        		},
        		{});
        // Put the result into something we can get at
        __templateTunnel.expandedTemplate = JSON.stringify(definition);

        // And include the prepopulated version with the parameter
        // values.  The default template validation doesn't accept
        // parameters, so if the defaultValues don't pass a validation
        // restriction, the validation will fail.
        __templateTunnel.parameterizedTemplate = JSON.stringify(__parameterizedTemplate(definition));
    };
}
/**

Encapsulates functions that handle embedding external resource files
into the expanded CloudFormation template.  Resources will be automatically
parsed and transformed into <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-join.html">
    Fn::Join</a> compatible representations.

@namespace Embed
*/
var Embed = {
    /**
     * Embed an external file
     * @param {String} pathArg  - Path, relative to template directory root, to embed
     */
    File: function ( /*pathArg , optionalPropertyBag */ ) {
        var fileArgs = Array.prototype.slice.call(arguments, 0);
        var filePath = fileArgs[0];
        var optionalPropertyBag = fileArgs[1];

        return function () {
            var resolvedPath = FileUtilsImpl.resolvedPath(filePath);
            var data = FileUtilsImpl.fileContents(resolvedPath);
            if (!_.isUndefined(optionalPropertyBag)) {
                var propertyBag = _.extend({}, {
                        tags: TAGS.toObject(),
                        params: PARAMS.toObject()
                    },
                    optionalPropertyBag);
                data = _.template(data)(propertyBag);
            }
            return Embed.Literal(data);
        }();
    },
    /**
     * Embed a string literal, properly parsed and composed into an <code>Fn:Join</code> representation
     * for CloudFormation
     * @param {String} stringContent String content to parse & embed
     */
    Literal: function(stringContent)
    {
        var parsed = EmbeddingUtilsImpl.Literal(stringContent);
        return JSON.parse(parsed);
    },
    /**
     * Embed an external file, including a property bag for expansion.  The property
     * bag will be augmented with <code>TAGS</code> and <code>PARAMS</code> objects from the provided
     * JSON input.  Template expansion happens in two phases: (1) during template evaluation
     * via the supplied propertyBag, and (2) at CloudInit start time via AWS CloudFormation
     * expansion.  Evaluation time parameters use <a href="http://underscorejs.org/#template" target="_blank">ERB-style</a> markup: <code>&lt;%= EXPAND_ME %&gt;</code> and AWS
     * CloudFormation params use Mustache-like syntax: <code>{{"Ref" : "AWS::Region"}}</code>
     *
     * @example <caption>Mixed property types</caption>
     * Include inline {{"Ref" : "AWS::Region"}} pseudo params for your {{"Ref" : "AWS::AccountId"}} template
     * including params that can be expanded at runtime like: <%= PARAMS.MyApplicationType %>
     *
     *
     * @param {String} pathArg  - Path, relative to template directory root, to embed
     * @param {Object} propertyBag - Property bag to use for template expression exapansion (<a href="http://underscorejs.org/#template" target="_blank">ERB-style</a>)
     */
    FileTemplate: function (pathArg, propertyBag) {
        return Embed.File(pathArg, propertyBag || {});
    },
    /**
     * Embed a JSON file
     * @param {string} pathArg Path to JSON file that should be embedded
     */
    JSONFile: function (pathArg) {
        return JSON.parse(FileUtilsImpl.fileContents(pathArg))
    }
};

/**
 * Create the set of cfn-init instructions necessary to Bootstrap a specific EC2 instance
 * @global
 * @param  {...initializationObjects} - Variable number of <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-init.html">CloudFormation::Init</a> blocks to sequence.
 */
CloudFormationInit = function ( /**arguents of objects representing init **/ ) {
    var initializers = Array.prototype.slice.call(arguments);
    // If there is a docker file, we need to include the necessary
    // pieces to install and monitor it...
    // packages, groups, users, sources, files, commands, and then services.
    if (PARAMS.get(CONSTANTS.PARAMETERS.KEYNAMES.DOCKER_IMAGE_PATH)) {
        logger.warn('DOCKER not yet supported');
        /*
        var JavaPath = Java.type("java.nio.file.Paths");
        var dockerPath = JavaPath.get(FileUtilsImpl.resolvedPath(PARAMS.get(CONSTANTS.PARAMETERS.KEYNAMES.DOCKER_IMAGE_PATH)));
        var fileParts = dockerPath.getFileName().toString().split(".");
        var imageName = fileParts[0] + "42" + (fileParts[1] || ".docker");

        var DOCKER_INIT = [
            {
                "files" : {
                    "/var/tmp/application.tar.gz": {
                        "source": {
                            "Fn::Join": [
                                "",
                                [
                                    "http://",
                                    {
                                        "Ref": "S3SourceBucketName"
                                    },
                                    ".s3.amazonaws.com/" + imageName
                                ]
                            ]
                        },
                        "mode": "000644",
                        "owner": "root",
                        "group": "root"
                    }
                },
                "commands": {
                    "0001_update_yum": {
                        "command": "yum update -y"
                    },
                    "0002_install_docker": {
                        "command": "yum install -y docker"
                    },
                    "0003_mkdir" : {
                        command: "mkdir -pv /var/tmp"
                    }
                },
                "services": {
                    "sysvinit": {
                        "docker": {
                            "enabled": "true",
                            "ensureRunning": "true"
                        }
                    }
                }
            },
            {
                "commands" :
                {
                    "0001_adduser" :
                    {
                        "command": "usermod -a -G docker ec2-user"
                    },
                    "0002_dockerinfo" :
                    {
                        "command" : "docker info"
                    },
                    "0003_unpack" :
                    {
                        "command" : "tar -xvf /var/tmp/application.tar.gz  --no-same-owner --directory /var/tmp/application"
                    },
                    "0004_run" :
                    {
                        "command" : "echo RUN ME!"
                    }
                }
            }
        ];
        initializers = initializers.concat(DOCKER_INIT);
        */
    }


    var defaultConfigSetKeys = [];
    var flattened = _.reduce(initializers,
        function (memo, eachInitializer) {
            var keyname = 'configSet' + _.keys(memo).length;
            defaultConfigSetKeys.push(keyname);

            // Each initializer is an object, but the
            // values in that block may be an array we need
            // to convert into properly sortable values.  This is mostly
            // helpful for commands
            eachInitializer = _.reduce(eachInitializer,
                function (initMemo, values, keyname) {
                    if (_.isArray(values)) {
                        var base = '00000000';
                        var offset = 1;
                        var ordered = _.reduce(values,
                            function (unorderedValues, initEntry) {
                                var nestedKeyname = base + offset;
                                nestedKeyname = nestedKeyname.slice(-1 * base.length);
                                // If this is a command entry, try to make a nice
                                // keyname in case an error is logged to
                                // /var/log/cfn-init.log
                                if (initEntry.command) {
                                    var parts = initEntry.command.split(/\s+/);
                                    if (parts.length > 1) {
                                        nestedKeyname += ('_' + parts[0]);
                                    }
                                }
                                unorderedValues[nestedKeyname] = initEntry;
                                return unorderedValues;
                            }, {});
                        values = ordered;
                    }
                    initMemo[keyname] = values;
                    return initMemo;
                }, {});
            memo[keyname] = eachInitializer;
            return memo;
        }, {});
    var result = {
        configSets: {}
    };
    result.configSets[CONSTANTS.CLOUDINIT.DEFAULT_CONFIGSET_KEYNAME] = defaultConfigSetKeys;
    return _.extend(result,
        flattened);
};
