/* global CloudFormationTemplate,EC2 */
CloudFormationTemplate("EC2InstanceWithSecurityGroupSample")({
  "AWSTemplateFormatVersion": "2010-09-09",
  "Description": "Validate the Lambda function",
  "Resources" :
  {
    "LambdaTest" : AWS.Lambda.Function(
     {
    	    "Code" : "./resources/simple",
    	    "S3Key" : "LambdaTest.zip",
    	    "Description" : "Simple AWS Lambda function",
    	    "Handler" : "index.handler"
     })
  }
});
