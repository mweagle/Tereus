var tappedLoad = load;

// Tap the global load function s.t. we can
// help out with relative path specs
load = function(pathArg)
{
    try
    {
        tappedLoad(pathArg);
    }
    catch (ex)
    {
        var resolved = FileUtils.resolvedPath(pathArg);
        logger.debug("Loading resolved path: " + resolved);
        tappedLoad(resolved);
    }
}

var PARAMS = {};
var TAGS = {};
(function initializer() {
    var args = JSON.parse(ArgumentsImpl());
    // Make sure that the args map includes any Tereus tags
    var tagNamespace = Java.type("com.mweagle.tereus.CONSTANTS").TEREUS_TAG_NAMESPACE;
    var commonTags = {};
    commonTags[tagNamespace + ":version"] = Java.type("com.mweagle.tereus.CONSTANTS").TEREUS_VERSION;

    args.tags = _.extend({},
                         args.tags || {},
                        commonTags);

    PARAMS = Immutable.Map(args.params || {});
    TAGS = Immutable.Map(args.tags || {});
})();

var amiValues = function (HVM_SSD_Backed, HVM_Instance_Store, PV_EBS_Backed, PV_Instance_Store) {
    return {
        'HVM_SSD': HVM_SSD_Backed,
        'HVM_INSTANCE': HVM_Instance_Store,
        'PV_EBS': PV_EBS_Backed,
        'PV_INSTANCE': PV_Instance_Store
    }
}

var CONSTANTS =
{
    "CLOUD_FORMATION": {
        'TEMPLATE_VERSION': '2010-09-09'
    },
    "MAPPINGS" : {
        "KEYNAMES" :
        {
            INSTANCE_TYPE_2_ARCH : "AWSInstanceType2Arch",
            REGION_ARCH_2_AMI : "AWSRegionArch2AMI"
        },
        "DEFINITIONS": {}
    },
    "PARAMETERS" : {
        "KEYNAMES" : {
            INSTANCE_TYPE: Java.type("com.mweagle.tereus.CONSTANTS").PARAMETER_NAMES.INSTANCE_TYPE,
            BUCKET_NAME: Java.type("com.mweagle.tereus.CONSTANTS").PARAMETER_NAMES.S3_BUCKET_NAME,
            DOCKER_IMAGE_PATH: Java.type("com.mweagle.tereus.CONSTANTS").PARAMETER_NAMES.DOCKER_IMAGE_PATH,
            KEY_NAME: Java.type("com.mweagle.tereus.CONSTANTS").PARAMETER_NAMES.SSH_KEY_NAME
        },
        "DEFINITIONS" : {}
    },
    "CLOUDINIT" : {
        DEFAULT_CONFIGSET_KEYNAME: "default"
    }
};

CONSTANTS.MAPPINGS.DEFINITIONS[CONSTANTS.MAPPINGS.KEYNAMES.INSTANCE_TYPE_2_ARCH] =
{
    "t1.micro": {"Arch": "PV64"},
    "t2.micro": {"Arch": "HVM64"},
    "t2.small": {"Arch": "HVM64"},
    "t2.medium": {"Arch": "HVM64"},
    "m1.small": {"Arch": "PV64"},
    "m1.medium": {"Arch": "PV64"},
    "m1.large": {"Arch": "PV64"},
    "m1.xlarge": {"Arch": "PV64"},
    "m2.xlarge": {"Arch": "PV64"},
    "m2.2xlarge": {"Arch": "PV64"},
    "m2.4xlarge": {"Arch": "PV64"},
    "m3.medium": {"Arch": "HVM64"},
    "m3.large": {"Arch": "HVM64"},
    "m3.xlarge": {"Arch": "HVM64"},
    "m3.2xlarge": {"Arch": "HVM64"},
    "c1.medium": {"Arch": "PV64"},
    "c1.xlarge": {"Arch": "PV64"},
    "c3.large": {"Arch": "HVM64"},
    "c3.xlarge": {"Arch": "HVM64"},
    "c3.2xlarge": {"Arch": "HVM64"},
    "c3.4xlarge": {"Arch": "HVM64"},
    "c3.8xlarge": {"Arch": "HVM64"},
    "c4.large": {"Arch": "HVM64"},
    "c4.xlarge": {"Arch": "HVM64"},
    "c4.2xlarge": {"Arch": "HVM64"},
    "c4.4xlarge": {"Arch": "HVM64"},
    "c4.8xlarge": {"Arch": "HVM64"},
    "g2.2xlarge": {"Arch": "HVMG2"},
    "r3.large": {"Arch": "HVM64"},
    "r3.xlarge": {"Arch": "HVM64"},
    "r3.2xlarge": {"Arch": "HVM64"},
    "r3.4xlarge": {"Arch": "HVM64"},
    "r3.8xlarge": {"Arch": "HVM64"},
    "i2.xlarge": {"Arch": "HVM64"},
    "i2.2xlarge": {"Arch": "HVM64"},
    "i2.4xlarge": {"Arch": "HVM64"},
    "i2.8xlarge": {"Arch": "HVM64"},
    "d2.xlarge": {"Arch": "HVM64"},
    "d2.2xlarge": {"Arch": "HVM64"},
    "d2.4xlarge": {"Arch": "HVM64"},
    "d2.8xlarge": {"Arch": "HVM64"},
    "hi1.4xlarge": {"Arch": "HVM64"},
    "hs1.8xlarge": {"Arch": "HVM64"},
    "cr1.8xlarge": {"Arch": "HVM64"},
    "cc2.8xlarge": {"Arch": "HVM64"}
};

CONSTANTS.MAPPINGS.DEFINITIONS[CONSTANTS.MAPPINGS.KEYNAMES.REGION_ARCH_2_AMI] =
{
    "us-east-1": {"PV64": "ami-1ccae774", "HVM64": "ami-10cae778", "HVMG2": "ami-8c6b40e4"},
    "us-west-2": {"PV64": "ami-ff527ecf", "HVM64": "ami-e9527ed9", "HVMG2": "ami-abbe919b"},
    "us-west-1": {"PV64": "ami-d514f291", "HVM64": "ami-cb14f28f", "HVMG2": "ami-f31ffeb7"},
    "eu-west-1": {"PV64": "ami-bf0897c8", "HVM64": "ami-a30897d4", "HVMG2": "ami-d5bc24a2"},
    "eu-central-1": {"PV64": "ami-ac221fb1", "HVM64": "ami-ae221fb3", "HVMG2": "ami-7cd2ef61"},
    "ap-northeast-1": {"PV64": "ami-27f90e27", "HVM64": "ami-c7f90ec7", "HVMG2": "ami-6318e863"},
    "ap-southeast-1": {"PV64": "ami-acd9e8fe", "HVM64": "ami-64d8e936", "HVMG2": "ami-3807376a"},
    "ap-southeast-2": {"PV64": "ami-ff9cecc5", "HVM64": "ami-f39cecc9", "HVMG2": "ami-89790ab3"},
    "sa-east-1": {"PV64": "ami-bb2890a6", "HVM64": "ami-b72890aa", "HVMG2": "NOT_SUPPORTED"},
    "cn-north-1": {"PV64": "ami-fa39abc3", "HVM64": "ami-e839abd1", "HVMG2": "NOT_SUPPORTED"}
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