/* global CONSTANTS,logger,AWSInfoImpl,UserInfoImpl,Embed,FileUtilsImpl,LambdaUtilsImpl,Immutable,_ */

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
 * <span class="label label-info">All Contexts</span><hr />
 * Global object representing UserInfo state, wrapped in an Immutable.JS map
 *
 * @type {Object}
 * @property {string} USER_INFO.arn - The ARN of the user currently executing Tereus
 * @property {string} USER_INFO.userId - The userID currently executing Tereus
 * @property {string} USER_INFO.name - The username currently executing Tereus
 * @property {string} USER_INFO.creationDate - The creation date of the user currently executing Tereus
 *
 * @example <caption>Accessing USER_INFO</caption>
 *
 * USER_INFO.get('arn') // => arn:aws:iam::000000000000:root
 *
 */
var USER_INFO = {
    arn: null,
    userId: null,
    name: null,
    creationDate:null
};

/**
 * <span class="label label-info">All Contexts</span><hr />
 * Global object storing AWS credentials & region, wrapped in an Immutable.JS map
 *
 * @type {Object}
 * @property {object} AWS_INFO.credentials - Object containing accessKey and secretAccessKey
 * @property {string} AWS_INFO.credentials.accessKeyId - AWS accessKeyId
 * @property {string} AWS_INFO.credentials.secretAccessKey - AWS secretAccessKey
 * @property {string} AWS_INFO.region - Target AWS region

 * @example <caption>Accessing AWS_INFO</caption>
 *
 * AWS_INFO.get('region') // => 'us-east-1'
 *
 */
var AWS_INFO = {
  credentials:
  {
    accessKeyId: null,
    secretAccessKey: null
  },
  region: null
};

////////////////////////////////////////////////////////////////////////////////
(function initializer() {
  try
  {
    var user = UserInfoImpl.getUser();
    var userInfoMap = {
        arn: user.getArn(),
        userId: user.getUserId(),
        name: user.getUserName(),
        creationDate: user.getCreateDate(),
    };
    USER_INFO = Immutable.Map(userInfoMap);
  }
  catch (ex)
  {
    logger.warn('Failed to initialize USER_INFO: ' + ex.toString());
  }
  try
  {
    // Turn that into a map
    AWS_INFO = Immutable.Map(JSON.parse(AWSInfoImpl()));
  }
  catch (ex)
  {
    logger.warn('Failed to initialize AWS UserInfo:' + ex.toString());
  }
})();

var tappedLoad = load;

/**
 * Tapped Nashorn <code>load</code> function that falls back to definition
 * scoped relative paths
 * @param  {string} pathArg Resolvable reference, or path relative to definition's
 *                          parent directory
 * @return {undefined}      Undefined - content is `eval`d in current execution context.
 */
var load = function(pathArg)
{
    // Find the first useful thing
    var exception = null;
    var candidates = [pathArg, FileUtilsImpl.resolvedPath(pathArg)];
    var loaded = false;
    for (var i = 0; i !== candidates.length && !loaded && !exception; ++i)
    {
      var resourcePath = candidates[i];
      try
      {
        logger.debug('Attempting path: ' + resourcePath);
        tappedLoad(resourcePath);
        logger.debug('Loaded resolved path: ' + resourcePath);
        loaded = true;
      }
      catch (e)
      {
        if (-1 !== e.toString().indexOf('SyntaxError'))
        {
          exception = e;
        }
      }
    }

    if (!loaded)
    {
      // Halt evaluation
      exception = exception || ('Unknown error for source: ' + pathArg);
      throw exception;
    }
};

// and change the Patch code to evaluate the template and
// check for resource changes...
var annotateTemplate = function(template, parameters, tags)
{
  parameters = parameters || {};
  tags = tags || {};

  // Ensure the template version
  template.AWSTemplateFormatVersion = CONSTANTS.CLOUD_FORMATION.TEMPLATE_VERSION;

  var tagResource = function (resourceDefinition) {
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
                        'AWS::S3::Bucket'];

        var resTags = null;
        if (!_.isEmpty(tags))
        {
          if (_.contains(taggable, resourceDefinition.Type)) {
              var extraTags = {};
              if ('AWS::AutoScaling::AutoScalingGroup' === resourceDefinition.Type) {
                  extraTags = {
                      'PropagateAtLaunch': true
                  };
              }

              resTags = _.map(tags,
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
        }
    };

    var lambdaFunction = function(resourceDefinition, logicalResourceName)
    {
        var lambdaTarget = ['AWS::Lambda::Function'];
        if (_.contains(lambdaTarget, resourceDefinition.Type) &&
            resourceDefinition.Properties)
        {
            var source = resourceDefinition.Properties.Code;
            logger.debug('Handling lambda: ' + logicalResourceName + ', source: ' + JSON.stringify(source, null, ' '));

            if (_.isString(source))
            {
                logger.debug('Creating Lambda function: ' + logicalResourceName);
                var bucket = parameters[CONSTANTS.PARAMETERS.KEYNAMES.BUCKET_NAME];
                var s3URL = JSON.parse(LambdaUtilsImpl.createFunction(logicalResourceName, source, bucket, resourceDefinition.Properties.S3Key || ''));
                resourceDefinition.Properties.Code = s3URL;
                // Make sure there's no S3Key at the root...
                if (resourceDefinition.Properties.S3Key)
                {
                    delete resourceDefinition.Properties.S3Key;
                }
            }
        }
        else
        {
          logger.debug('Lambda annotate doesn\'t apply to: ' + logicalResourceName);
        }
    };

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

    var cloudInitInstances = function (resourceDefinition, logicalResourceName) {

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

    ////////////////////////////////////////////////////////////////////////
    // BEGIN annotators
    logger.info('Annotating template');
    var annotators = [tagResource,
                      cloudInitInstances,
                      lambdaFunction];


    // Apply the annotator to each resource
    _.each(template.Resources || {},
           function (resourceDefinition, logicalName)
           {
            _.each(annotators, function (eachAnnotator)
            {
              eachAnnotator(resourceDefinition, logicalName);
            });
          });
  return template;
};