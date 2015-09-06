/* global logger,annotateTemplate,__templateTunnel,_,PARAMS,TAGS,CONSTANTS,FileUtilsImpl,Java,LambdaUtilsImpl */
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


/**
 * <span class="label label-info">Creation Context</span><hr />
 *
 * The global CloudFormationTemplate function responsible
 * for expanding the inline stack definition.
 *
 *
 * @param {string} stackName - Name of the Stack
 */
function CloudFormationTemplate(stackName) {
    __templateTunnel.stackName = stackName;

    var __parameterizedTemplate = function (definition) {
        var params = definition.Parameters || {};
        _.each(params, function (eachValue, eachKey) {
            var paramValue = PARAMS.get(eachKey) || eachValue.Default || undefined;
            eachValue.Default = paramValue;
        });
        return definition;
    };

    return function (definition) {

        // Annotate the template
        definition = annotateTemplate(definition, PARAMS.toObject(), TAGS.toObject());

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
<span class="label label-info">Creation Context</span><hr />

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
    	var embeddingUtils = Java.type('com.mweagle.tereus.commands.utils.EmbeddingUtils');
        var parsed = embeddingUtils.Literal(stringContent);
        return JSON.parse(parsed);
    },
    /**
     * Embed an external file, including a property bag for expansion.  The property
     * bag will be augmented with <code>TAGS</code> and <code>PARAMS</code> objects from the provided
     * JSON input.  Template expansion happens in two phases: (1) during template evaluation
     * via the supplied propertyBag, and (2) at CloudInit start time via AWS CloudFormation
     * expansion.  Evaluation time parameters use <a href="http://underscorejs.org/#template" target="_blank">ERB-style</a>
     *             markup: <code>&lt;%= EXPAND_ME %&gt;</code> and AWS
     * CloudFormation params use Mustache-like syntax: <code>{{"Ref" : "AWS::Region"}}</code>
     *
     * @example <caption>Mixed property types</caption>
     * Include inline {{"Ref" : "AWS::Region"}} pseudo params for your {{"Ref" : "AWS::AccountId"}} template
     * including params that can be expanded at runtime like: <%= PARAMS.MyApplicationType %>
     *
     *
     * @param {String} pathArg  - Path, relative to template directory root, to embed
     * @param {Object} propertyBag - Property bag to use for template expression exapansion
     *                               (<a href="http://underscorejs.org/#template" target="_blank">ERB-style</a>)
     */
    FileTemplate: function (pathArg, propertyBag) {
        return Embed.File(pathArg, propertyBag || {});
    },
    /**
     * Embed a JSON file
     * @param {string} pathArg Path to JSON file that should be embedded
     */
    JSONFile: function (pathArg) {
        return JSON.parse(FileUtilsImpl.fileContents(pathArg));
    }
};

/**
 * <span class="label label-info">Creation Context</span><hr />
 * Create the set of cfn-init instructions necessary to Bootstrap a specific EC2 instance
 * @global
 * @param  {...initializationObjects} - Variable number of
 *                                      <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-init.html">CloudFormation::Init</a>
 *                                      blocks to sequence.
 */
var CloudFormationInit = function ( /**arguents of objects representing init **/ ) {
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
