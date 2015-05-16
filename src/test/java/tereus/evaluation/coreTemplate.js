CloudFormationTemplate("Test")({
    "Description": "Test",
    "Resources": {
        "EC2Instance": {
            "Metadata" : {
                "Templated" : "Literal"
            },
            "Type": "AWS::EC2::Instance",
            "Properties": EC2.Properties()
        }
    },
    "Parameters" : _.reduce([CONSTANTS.PARAMETERS.KEYNAMES.INSTANCE_TYPE],
        function (memo, keyname)
        {
            memo[keyname] = CONSTANTS.PARAMETERS.DEFINITIONS[keyname];
            return memo;
        },
        {}),
    "Outputs": {
        "InstanceId": {
            "Description": "InstanceId of the newly created EC2 instance",
            "Value": {"Ref": "EC2Instance"}
        }
    }
});