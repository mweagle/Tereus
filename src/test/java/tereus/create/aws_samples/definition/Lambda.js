/* global CloudFormationTemplate,AWS,USER_INFO */

var lambdaS3KeyName = function()
{
	var JavaString = Java.type("java.lang.String");
	var awsUserName = USER_INFO.get('arn').split(':').pop();
	return JavaString.format("Lambda-%s.zip", awsUserName);
};

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
    	    "S3Key" : lambdaS3KeyName(),
    	    "Description" : "Simple AWS Lambda function",
    	    "Handler" : "index.handler"
     })
  }
});
