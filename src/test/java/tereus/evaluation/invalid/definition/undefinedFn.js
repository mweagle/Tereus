CloudFormationTemplate("Test")({
    "Description": "Test Unreferenced function",
    "Resources": {
        "EC2Instance": {
            "Type": "AWS::EC2::Instance",
            "Properties": EC2.UndefinedFunction()
        }
    },
    "Outputs": {
        "InstanceId": {
            "Description": "InstanceId of the newly created EC2 instance",
            "Value": {"Ref": "EC2Instance"}
        }
    }
});