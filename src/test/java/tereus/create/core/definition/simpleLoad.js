load("resources/simpleLoad.js");

CloudFormationTemplate("Test")({
    "Description": SimpleLoad.Name(),
    "Resources": {
        "Hello": "World"
    },
    "Outputs": {}
});
