CloudFormationTemplate("Test")({
    "Description": "Test",
    "Resources": {
        "Templated": Embed.File("resources/embeddable.txt")
    },
    "Outputs": {}
});
