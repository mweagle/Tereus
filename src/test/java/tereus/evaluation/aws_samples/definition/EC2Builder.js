/* global CloudFormationTemplate,EC2 */
CloudFormationTemplate("EC2InstanceWithSecurityGroupSample")({
  "AWSTemplateFormatVersion": "2010-09-09",
  "Description": "Validate the EC2 helper object",
  "Resources" :
  {
    "MyEC2Instance" : AWS.EC2.WithProperties({
        "Metadata": {
          "Builder":"Created"
        }
      })
  }
});
