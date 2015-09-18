/* global CloudFormationTemplate,AWS */
CloudFormationTemplate("TereusLambdaExample")({
    "Description": "Deploy a Lambda function",
    "Resources" :
    {
        "TereusExampleLambda" : AWS.Lambda.Function(
        {
            "Code" : "./original_source",
            "Description" : "Simple AWS Lambda function that requires `npm install`",
            "Handler" : "index.handler"
        })
    }
});
