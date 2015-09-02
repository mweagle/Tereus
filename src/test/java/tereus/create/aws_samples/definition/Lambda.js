/* global CONSTANTS,CloudFormationTemplate,AWS */

CloudFormationTemplate("Lambda")({
  "AWSTemplateFormatVersion": "2010-09-09",
  "Description": "Validate the Lambda function",
  "Parameters" :
	  {
	  	"BucketName" : CONSTANTS.PARAMETERS.DEFINITIONS.BucketName
	  },
  "Resources" :
  {
	 "LambdaRole" : AWS.IAM.LambdaRole,
     "LambdaTest" : AWS.Lambda.Function(
     {
    	    "Code" : "./resources/simple",
    	    "Role" : {"Fn::GetAtt" : ["LambdaRole", "Arn"]},
    	    "S3Key" : "Lambda-test.zip",
    	    "Description" : "Simple AWS Lambda function",
    	    "Handler" : "index.handler"
     })
  }
});
