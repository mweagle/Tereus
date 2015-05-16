/* global __templateTunnel */

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
            //var configSets = (init[0] && init[0].configSets) ? init[0].configSets[CONSTANTS.CLOUDINIT.DEFAULT_CONFIGSET_KEYNAME] : [];
            // TODO - Add the key to the configset and then the S3 download, registration, and unpacking.
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

        /*
        TODO - If there is a Docker file in the params, then add
        the cloudinit section to all the EC2 resources
       **/
        _.each(definition.Resources, __cloudInitInstances.bind(this));

        // Put the result into something we can get at
        __templateTunnel.expandedTemplate = JSON.stringify(definition);

        // And include the prepopulated version with the parameter
        // values.  The default template validation doesn't accept
        // parameters, so if the defaultValues don't pass a validation
        // restriction, the validation will fail.
        __templateTunnel.parameterizedTemplate = JSON.stringify(__parameterizedTemplate(definition));
    }
}

Embed =
{
    File: function(pathArg /*, optionalPropertyBag */)
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
        }()
    },
    FileTemplate: function(pathArg, userPropertyBag)
    {
        return Embed.File(pathArg, userPropertyBag);
    },
    JSONFile : function(pathArg)
    {
        return JSON.parse(FileUtils.fileContents(pathArg))
    }
};

ConditionalResource = function (fnPredicate, resourceValue) {
    return function () {
        var data = _.isFunction(resourceValue) ?
            resourceValue.call(null) : resourceValue;
        return (fnPredicate() ? data : undefined);
    }();
};

ParamConditionalResource = function (paramName, paramValue, value) {
    return ConditionalResource(function () {
            return PARAMS.get(paramName) === paramValue
        },
        value);
};

TagConditionalResource = function (tagName, tagValue, value) {
    return ConditionalResource(function () {
            return TAGS.get(tagName) === tagValue
        },
        value);
};

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

CloudFormationInit = function (/**arguents of objects representing init **/) {
    var initializers = Array.prototype.slice.call(arguments);
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

var EC2 =
{
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
}

