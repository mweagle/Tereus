/* global CloudFormationUpdate,Patch */
/**
 * Don't specify a stack name target in the CloudFormationUpdate, since
 * we don't have a predefined stack to target.
 */
CloudFormationUpdate()({
  "Key":
  {
    "Subkey" : Patch.Add("Value")
  }
});