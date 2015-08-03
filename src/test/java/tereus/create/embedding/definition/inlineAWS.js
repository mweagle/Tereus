CloudFormationTemplate("Test")({
    "Description": "Test",
    "Resources": {
        "Templated": Embed.File("resources/inlineAWS.txt")
    },
    "Outputs": {}
});
