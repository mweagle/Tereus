CloudFormationTemplate("Test")({
    "Description": "Test",
    "Resources": {
        "Templated": Embed.FileTemplate("resources/embeddable.template", {EXPAND_ME: "Nashorn"})
    },
    "Outputs": {}
});
