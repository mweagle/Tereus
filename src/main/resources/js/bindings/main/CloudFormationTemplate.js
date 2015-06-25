/* global __templateTunnel,_,PARAMS,TAGS,CONSTANTS,FileUtils,EmbeddingUtils */


CfnInitUserData = function(logicalResourceName)
{
    var data = ["#!/bin/bash -xe",
                "yum update -y aws-cfn-bootstrap",
                "/opt/aws/bin/cfn-init -v --stack {{ \"Ref\" : \"AWS::StackName\" }} --resource <%= logicalResourceName %> --configsets <%= configsetName %> --region {{ \"Ref\" : \"AWS::Region\" }}"].join("\n");
    data = _.template(data)({
        logicalResourceName: logicalResourceName,
        configsetName: CONSTANTS.CLOUDINIT.DEFAULT_CONFIGSET_KEYNAME
    });

    var parsed = EmbeddingUtils.Literal(data);
    return JSON.parse(parsed);
};

/**
 * The global CloudFormationTemplate function responsible
 * for expanding the inline stack definition.
 *
 * @param {string} stackName - Name of the Stack
 */
function CloudFormationTemplate(stackName) {
    __templateTunnel.stackName = stackName;

    var __tagResource = function(resourceDefinition)
    {
        var taggable = ["AWS::AutoScaling::AutoScalingGroup",
            "AWS::EC2::CustomerGateway",
            "AWS::EC2::Instance",
            "AWS::EC2::InternetGateway",
            "AWS::EC2::NetworkAcl",
            "AWS::EC2::NetworkInterface",
            "AWS::EC2::RouteTable",
            "AWS::EC2::SecurityGroup",
            "AWS::EC2::Subnet",
            "AWS::EC2::Volume",
            "AWS::EC2::VPC",
            "AWS::EC2::VPCPeeringConnection",
            "AWS::EC2::VPNConnection",
            "AWS::EC2::VPNGateway",
            "AWS::ElasticLoadBalancing::LoadBalancer",
            "AWS::RDS::DBInstance",
            "AWS::RDS::DBParameterGroup",
            "AWS::RDS::DBSubnetGroup",
            "AWS::RDS::DBSecurityGroup",
            "AWS::S3::Bucket"];

        var resTags = null;

        if (_.contains(taggable, resourceDefinition.Type))
        {
            var extraTags = {};
            if ("AWS::AutoScaling::AutoScalingGroup" === resourceDefinition.Type)
            {
                extraTags =
                {
                    "PropagateAtLaunch" : true
                };
            }

            resTags = _.map(TAGS.toObject(),
                function (eachTagValue, eachTagName)
                {
                    return _.extend({},
                        extraTags,
                        {
                            "Key": eachTagName,
                            "Value" : eachTagValue
                        });
                });
        }
        if (resTags)
        {
            resourceDefinition.Properties.Tags = _.extend(resTags,
                resourceDefinition.Properties.Tags || {});
        }
    };

    var __cloudInitInstances = function(resourceDefinition, logicalResourceName)
    {

        var cloudInitTarget = ["AWS::AutoScaling::AutoScalingGroup",
            "AWS::EC2::Instance"];
        if (_.contains(cloudInitTarget, resourceDefinition.Type))
        {
            var metadata = resourceDefinition.Metadata || {};
            var init = metadata["AWS::CloudFormation::Init"] || null;
            if (init)
            {

                // Ensure that the Userdata Cfn-init action is hooked up...
                var properties = resourceDefinition.Properties || {};
                if (_.isEmpty(properties.UserData))
                {
                    properties.UserData =  CfnInitUserData(logicalResourceName);
                }
                resourceDefinition.Properties = properties;
            }
        }
    };
    var __parameterizedTemplate = function(definition)
    {
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

        // Scan the resources and insert tags for anything that might have a tag
        _.each(definition.Resources, __tagResource.bind(this));

        _.each(definition.Resources, __cloudInitInstances.bind(this));

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
var Embed =
{
    /**
     * Embed an external file
     * @param {String} pathArg  - Path, relative to template directory root, to embed
     */
    File: function(/*pathArg , optionalPropertyBag */)
    {
        var fileArgs = Array.prototype.slice.call(arguments, 0);
        var filePath = fileArgs[0];
        var optionalPropertyBag = fileArgs[1];

        return function() {
            var resolvedPath = FileUtils.resolvedPath(filePath);
            var data  = FileUtils.fileContents(resolvedPath);
            if (!_.isUndefined(optionalPropertyBag))
            {
                var propertyBag = _.extend({},
                    {
                        tags: TAGS.toObject(),
                        params: PARAMS.toObject()
                    },
                    optionalPropertyBag);
                data = _.template(data)(propertyBag);
            }
            var parsed = EmbeddingUtils.Literal(data);
            return JSON.parse(parsed);
        }();
    },
    /**
     * Embed an external file, including a property bag for expansion.  The property
     * bag will be augmented with <code>TAGS</code> and <code>PARAMS</code> objects from the provided
     * JSON input.  Template expansion happens in two phases: (1) during template evaluation
     * via the supplied propertyBag, and (2) at CloudInit start time via AWS CloudFormation
     * expansion.  Evaluation time parameters use ERB-style markup: <code>&lt;%= EXPAND_ME %&gt;</code> and AWS
     * CloudFormation params use Mustache-like syntax: <code>{{"Ref" : "AWS::Region"}}</code>
     *
     * @example <caption>Mixed property types</caption>
     * Include inline {{"Ref" : "AWS::Region"}} pseudo params for your {{"Ref" : "AWS::AccountId"}} template
     * including params that can be expanded at runtime like: <%= PARAMS.MyApplicationType %>
     *
     *
     * @param {String} pathArg  - Path, relative to template directory root, to embed
     * @param {Object} propertyBag - Property bag to use for template expression exapansion (ERB-style)
     */
    FileTemplate: function(pathArg, propertyBag)
    {
        return Embed.File(pathArg, propertyBag);
    },
    /**
     * Embed a JSON file
     * @param {string} pathArg Path to JSON file that should be embedded
     */
    JSONFile : function(pathArg)
    {
        return JSON.parse(FileUtils.fileContents(pathArg))
    }
};

/**
 * Create the set of cfn-init instructions necessary to Bootstrap a specific EC2 instance
 * @global
 * @param  {...initializationObjects} - Variable number of <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-init.html">CloudFormation::Init</a> blocks to sequence.
 */
CloudFormationInit = function (/**arguents of objects representing init **/) {
    var initializers = Array.prototype.slice.call(arguments);
    // If there is a docker file, we need to include the necessary
    // pieces to install and monitor it...
    // packages, groups, users, sources, files, commands, and then services.
    if (PARAMS.get(CONSTANTS.PARAMETERS.KEYNAMES.DOCKER_IMAGE_PATH))
    {
        var JavaPath = Java.type("java.nio.file.Paths");
        var dockerPath = JavaPath.get(FileUtils.resolvedPath(PARAMS.get(CONSTANTS.PARAMETERS.KEYNAMES.DOCKER_IMAGE_PATH)));
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
                        /**
                        TODO - Import the image and start it up.
                        **/
                        "command" : "echo RUN ME!"
                    }
                }
            }
        ];
        initializers = initializers.concat(DOCKER_INIT);
    }


    var defaultConfigSetKeys = [];
    var flattened = _.reduce(initializers,
        function (memo, eachInitializer) {
            var keyname = "configSet" + _.keys(memo).length;
            defaultConfigSetKeys.push(keyname);

            // Each initializer is an object, but the
            // values in that block may be an array we need
            // to convert into properly sortable values.  This is mostly
            // helpful for commands
            eachInitializer = _.reduce(eachInitializer,
                function (initMemo, values, keyname) {
                    if (_.isArray(values)) {
                        var base = "00000000";
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
                                        nestedKeyname += ("_" + parts[0]);
                                    }
                                }
                                unorderedValues[nestedKeyname] = initEntry;
                                return unorderedValues;
                            },
                            {});
                        values = ordered;
                    }
                    initMemo[keyname] = values;
                    return initMemo;
                }, {});
            memo[keyname] = eachInitializer;
            return memo;
        },
        {});
    var result = {
        configSets: {}
    }
    result.configSets[CONSTANTS.CLOUDINIT.DEFAULT_CONFIGSET_KEYNAME] = defaultConfigSetKeys;
    return _.extend(result,
        flattened);
}


/**

Encapsulates functions that are convenience builders for creating
<a href="http://docs.aws.amazon.com/search/doc-search.html?searchPath=documentation-guide&searchQuery=EC2&x=0&y=0&this_doc_product=AWS+CloudFormation&this_doc_guide=User+Guide&doc_locale=en_us#facet_doc_product=AWS%20CloudFormation&facet_doc_guide=User%20Guide">EC2</a>
CloudFormation resources.

@namespace EC2
*/
var EC2 =
{
    /**
     * Return default EC2 properties object required to create
     * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-instance.html">AWS::EC2::Instance</a>
     *
     * @param {Object} additionalUserProps  - Additional user properties to compose with defaults.
     */
    Properties: function (additionalUserProps) {
        return _.extend({
                            "InstanceType": {
                                "Ref": CONSTANTS.PARAMETERS.KEYNAMES.INSTANCE_TYPE
                            },
                            "KeyName": {
                                "Ref": CONSTANTS.PARAMETERS.KEYNAMES.KEY_NAME
                            },
                            "ImageId": {
                                "Fn::FindInMap": [CONSTANTS.MAPPINGS.KEYNAMES.REGION_ARCH_2_AMI, {
                                    "Ref": "AWS::Region"
                                }, {
                                    "Fn::FindInMap": [CONSTANTS.MAPPINGS.KEYNAMES.INSTANCE_TYPE_2_ARCH, {
                                        "Ref": CONSTANTS.PARAMETERS.KEYNAMES.INSTANCE_TYPE
                                    }, "Arch"]
                                }]
                            }
                        }, additionalUserProps || {});
    }
};

