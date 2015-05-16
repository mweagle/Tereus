CloudFormationTemplate("Test")({
    "Description": "Test",
    "Resources": {
        "EC2Instance": {
            "Metadata" :
            {
                "AWS::CloudFormation::Init" :
                    CloudFormationInit(Embed.JSONFile("resources/cloudinit1.json"),
                                        Embed.JSONFile("resources/cloudinit2.json"))
            },
            "Type": "AWS::EC2::Instance",
            "Properties" : {}
        }
    },
    "Outputs": {}
});