/* global CloudFormationTemplate,AWS */
CloudFormationTemplate("LambdaGradleBuild")({
  "AWSTemplateFormatVersion": "2010-09-09",
  "Description": "Validate the Lambda function",
  "Resources" :
  {
    "LambdaTest" : AWS.Lambda.Function(
     {
          "Code" : "./resources/gradlebuild",
          "S3Key" : "LambdaInstallTest.zip",
          "Description" : "Simple AWS Lambda function that requires `gradle build`",
          "Handler" : "example.Hello::myHandler"
     })
  }
});
