/* global Java,ArgumentsImpl,Immutable,_ */
// Copyright (c) 2015 Matt Weagle (mweagle@gmail.com)

// Permission is hereby granted, free of charge, to
// any person obtaining a copy of this software and
// associated documentation files (the 'Software'),
// to deal in the Software without restriction,
// including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so,
// subject to the following conditions:

// The above copyright notice and this permission
// notice shall be included in all copies or substantial
// portions of the Software.

// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF
// ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
// SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE
/**
 * Global template evaluation Parameter values.  Parameters
 * are wrapped as an <a href="https://facebook.github.io/immutable-js/">immutable
 * object</a>.
 *
 * @example <caption>Accessing PARAMS</caption>
 *
 * PARAMS.get('someProperty')
 *
 * @type {Object}
 */
var PARAMS = {};

/**
 * Global template evaluation Tag values.  Tags
 * are wrapped as an <a href="https://facebook.github.io/immutable-js/">immutable
 * object</a>.  All CloudFormation resources that support
 * <a href="http://docs.aws.amazon.com/search/doc-search.html?searchPath=documentation-guide&searchQuery=tags&x=0&y=0&this_doc_product=AWS+CloudFormation&this_doc_guide=User+Guide&doc_locale=en_us#facet_doc_product=AWS%20CloudFormation&facet_doc_guide=User%20Guide">Tags</a> will be
 * annotated with the supplied TAGS.
 *
 * @example <caption>Accessing TAGS</caption>
 *
 * TAGS.get('myApplicationVersion')
 *
 * @type {Object}
 */
var TAGS = {};
(function initializer() {
    var args = JSON.parse(ArgumentsImpl());
    // Make sure that the args map includes any Tereus tags
    var tagNamespace = Java.type('com.mweagle.tereus.CONSTANTS').TEREUS_TAG_NAMESPACE;
    var commonTags = {};
    commonTags[tagNamespace + ':version'] = Java.type('com.mweagle.tereus.CONSTANTS').TEREUS_VERSION;

    args.tags = _.extend({},
                         args.tags || {},
                        commonTags);

    PARAMS = Immutable.Map(args.params || {});
    TAGS = Immutable.Map(args.tags || {});
})();

/**
 * Global constants
 * @type {Object}
 * @global
 * @property {string} CLOUD_FORMATION.TEMPLATE_VERSION - The AWS CloudFormation <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/format-version-structure.html">Format Version</a>
 * @property {string} MAPPINGS.KEYNAMES.REGION_ARCH_2_AMI - The default Mapping keyname
 */
var CONSTANTS =
{
    'CLOUD_FORMATION': {
        'TEMPLATE_VERSION': '2010-09-09'
    },
    'MAPPINGS' : {
        'KEYNAMES' :
        {
            INSTANCE_TYPE_2_ARCH : 'AWSInstanceType2Arch',
            REGION_ARCH_2_AMI : 'AWSRegionArch2AMI'
        },
        'DEFINITIONS': {}
    },
    'PARAMETERS' : {
        'KEYNAMES' : {
            INSTANCE_TYPE: Java.type('com.mweagle.tereus.CONSTANTS').PARAMETER_NAMES.INSTANCE_TYPE,
            BUCKET_NAME: Java.type('com.mweagle.tereus.CONSTANTS').PARAMETER_NAMES.S3_BUCKET_NAME,
            DOCKER_IMAGE_PATH: Java.type('com.mweagle.tereus.CONSTANTS').PARAMETER_NAMES.DOCKER_IMAGE_PATH,
            KEY_NAME: Java.type('com.mweagle.tereus.CONSTANTS').PARAMETER_NAMES.SSH_KEY_NAME
        },
        'DEFINITIONS' : {}
    },
    'CLOUDINIT' : {
        DEFAULT_CONFIGSET_KEYNAME: 'default'
    }
};

CONSTANTS.MAPPINGS.DEFINITIONS[CONSTANTS.MAPPINGS.KEYNAMES.INSTANCE_TYPE_2_ARCH] =
{
    't1.micro': {'Arch': 'PV_EBS'},
    't2.micro': {'Arch': 'HVM_EBS'},
    't2.small': {'Arch': 'HVM_EBS'},
    't2.medium': {'Arch': 'HVM_EBS'},
    'm1.small': {'Arch': 'PV_EBS'},
    'm1.medium': {'Arch': 'PV_EBS'},
    'm1.large': {'Arch': 'PV_EBS'},
    'm1.xlarge': {'Arch': 'PV_EBS'},
    'm2.xlarge': {'Arch': 'PV_EBS'},
    'm2.2xlarge': {'Arch': 'PV_EBS'},
    'm2.4xlarge': {'Arch': 'PV_EBS'},
    'm3.medium': {'Arch': 'HVM_EBS'},
    'm3.large': {'Arch': 'HVM_EBS'},
    'm3.xlarge': {'Arch': 'HVM_EBS'},
    'm3.2xlarge': {'Arch': 'HVM_EBS'},
    'c1.medium': {'Arch': 'PV_EBS'},
    'c1.xlarge': {'Arch': 'PV_EBS'},
    'c3.large': {'Arch': 'HVM_EBS'},
    'c3.xlarge': {'Arch': 'HVM_EBS'},
    'c3.2xlarge': {'Arch': 'HVM_EBS'},
    'c3.4xlarge': {'Arch': 'HVM_EBS'},
    'c3.8xlarge': {'Arch': 'HVM_EBS'},
    'c4.large': {'Arch': 'HVM_EBS'},
    'c4.xlarge': {'Arch': 'HVM_EBS'},
    'c4.2xlarge': {'Arch': 'HVM_EBS'},
    'c4.4xlarge': {'Arch': 'HVM_EBS'},
    'c4.8xlarge': {'Arch': 'HVM_EBS'},
    'g2.2xlarge': {'Arch': 'HVM_G2'},
    'r3.large': {'Arch': 'HVM_EBS'},
    'r3.xlarge': {'Arch': 'HVM_EBS'},
    'r3.2xlarge': {'Arch': 'HVM_EBS'},
    'r3.4xlarge': {'Arch': 'HVM_EBS'},
    'r3.8xlarge': {'Arch': 'HVM_EBS'},
    'i2.xlarge': {'Arch': 'HVM_EBS'},
    'i2.2xlarge': {'Arch': 'HVM_EBS'},
    'i2.4xlarge': {'Arch': 'HVM_EBS'},
    'i2.8xlarge': {'Arch': 'HVM_EBS'},
    'd2.xlarge': {'Arch': 'HVM_EBS'},
    'd2.2xlarge': {'Arch': 'HVM_EBS'},
    'd2.4xlarge': {'Arch': 'HVM_EBS'},
    'd2.8xlarge': {'Arch': 'HVM_EBS'},
    'hi1.4xlarge': {'Arch': 'HVM_EBS'},
    'hs1.8xlarge': {'Arch': 'HVM_EBS'},
    'cr1.8xlarge': {'Arch': 'HVM_EBS'},
    'cc2.8xlarge': {'Arch': 'HVM_EBS'}
};


CONSTANTS.MAPPINGS.DEFINITIONS[CONSTANTS.MAPPINGS.KEYNAMES.REGION_ARCH_2_AMI] = {
    'us-east-1': {
        'HVM_EBS': 'ami-1ecae776',
        'HVM_G2': 'ami-28cae740',
        'PV_EBS': 'ami-1ccae774',
        'PV_IS': 'ami-5ccae734'
    },
    'us-west-2': {
        'HVM_EBS': 'ami-e7527ed7',
        'HVM_G2': 'ami-9f527eaf',
        'PV_EBS': 'ami-ff527ecf',
        'PV_IS': 'ami-97527ea7'
    },
    'us-west-1': {
        'HVM_EBS': 'ami-d114f295',
        'HVM_G2': 'ami-3b14f27f',
        'PV_EBS': 'ami-d514f291',
        'PV_IS': 'ami-3714f273'
    },
    'eu-west-1': {
        'HVM_EBS': 'ami-a10897d6',
        'HVM_G2': 'ami-c90897be',
        'PV_EBS': 'ami-bf0897c8',
        'PV_IS': 'ami-cf0897b8'
    },
    'eu-central-1': {
        'HVM_EBS': 'ami-a8221fb5',
        'HVM_G2': 'ami-b0221fad',
        'PV_EBS': 'ami-ac221fb1',
        'PV_IS': 'ami-b6221fab'
    },
    'ap-southeast-1': {
        'HVM_EBS': 'ami-68d8e93a',
        'HVM_G2': 'ami-32d8e960',
        'PV_EBS': 'ami-acd9e8fe',
        'PV_IS': 'ami-1cd8e94e'
    },
    'ap-northeast-1': {
        'HVM_EBS': 'ami-cbf90ecb',
        'HVM_G2': 'ami-ddfa0ddd',
        'PV_EBS': 'ami-27f90e27',
        'PV_IS': 'ami-d5fa0dd5'
    },
    'ap-southeast-2': {
        'HVM_EBS': 'ami-fd9cecc7',
        'HVM_G2': 'ami-fb9cecc1',
        'PV_EBS': 'ami-ff9cecc5',
        'PV_IS': 'ami-819cecbb'
    },
    'sa-east-1': {
        'HVM_EBS': 'ami-b52890a8',
        'HVM_G2': 'ami-bd2890a0',
        'PV_EBS': 'ami-bb2890a6',
        'PV_IS': 'ami-bf2890a2'
    },
    'cn-north-1': {
        'HVM_EBS': 'ami-f239abcb',
        'HVM_G2': 'ami-f639abcf',
        'PV_EBS': 'ami-fa39abc3',
        'PV_IS': 'ami-f439abcd'
    },
    'gov': {
        'HVM_EBS': 'ami-41b2d362',
        'HVM_G2': 'ami-7db2d35e',
        'PV_EBS': 'ami-47b2d364',
        'PV_IS': 'ami-75b2d356'
    }
};

CONSTANTS.MAPPINGS.DEFAULT = _.reduce(Object.keys(CONSTANTS.MAPPINGS.KEYNAMES),
    function (memo, keyname)
    {
        memo[keyname] =
            CONSTANTS.MAPPINGS.DEFINITIONS[keyname];
        return memo;
    },
    {});

CONSTANTS.PARAMETERS.DEFINITIONS[CONSTANTS.PARAMETERS.KEYNAMES.KEY_NAME] = {
    'Description': 'Name of an existing EC2 KeyPair',
    'Type': 'String',
    'MinLength': '1',
    'MaxLength': '255',
    'AllowedPattern': '[\\x20-\\x7E]*',
    'ConstraintDescription': 'can contain only ASCII characters.'
};

CONSTANTS.PARAMETERS.DEFINITIONS[CONSTANTS.PARAMETERS.KEYNAMES.BUCKET_NAME] = {
    'Description': 'Name of an S3 bucket that includes the Docker image to deploy',
    'Type': 'String',
    'Default': 'S3 Bucket Name in Stack Region',
    'MinLength': '1',
    'MaxLength': '255',
    'AllowedPattern': '[\\x20-\\x7E]*',
    'ConstraintDescription': 'can contain only ASCII characters.'
};

CONSTANTS.PARAMETERS.DEFINITIONS[CONSTANTS.PARAMETERS.KEYNAMES.DOCKER_IMAGE_PATH] = {
    'Description': 'Docker image (docker --save --output)',
        'Type': 'String',
        'Default': 'dockerImagePath',
        'MinLength': '0',
        'MaxLength': '255',
        'AllowedPattern': '[\\x20-\\x7E]*',
        'ConstraintDescription': 'can contain only ASCII characters.'
};

CONSTANTS.PARAMETERS.DEFINITIONS[CONSTANTS.PARAMETERS.KEYNAMES.INSTANCE_TYPE] = {
    'Description': 'EC2 Instance Type',
        'Type': 'String',
        'Default': 'm1.small',
        'AllowedValues': [
        't2.micro',
        't1.micro',
        't2.small',
        'm1.small',
        't2.medium',
        'm3.medium',
        'm1.medium',
        'c3.large',
        'c4.large',
        'c1.medium',
        'm3.large',
        'm1.large',
        'r3.large',
        'c3.xlarge',
        'c4.xlarge',
        'm2.xlarge',
        'm3.xlarge',
        'm1.xlarge',
        'r3.xlarge',
        'c3.2xlarge',
        'c4.2xlarge',
        'm2.2xlarge',
        'c1.xlarge',
        'm3.2xlarge',
        'g2.2xlarge',
        'd2.xlarge',
        'r3.2xlarge',
        'c3.4xlarge',
        'i2.xlarge',
        'c4.4xlarge',
        'm2.4xlarge',
        'd2.2xlarge',
        'r3.4xlarge',
        'c3.8xlarge',
        'i2.2xlarge',
        'c4.8xlarge',
        'cc2.8xlarge',
        'cg1.4xlarge',
        'd2.4xlarge',
        'r3.8xlarge',
        'hi1.4xlarge',
        'i2.4xlarge',
        'cr1.8xlarge',
        'hs1.8xlarge',
        'd2.8xlarge',
        'i2.8xlarge'],
        'ConstraintDescription': 'must be a valid EC2 instance type.'
};