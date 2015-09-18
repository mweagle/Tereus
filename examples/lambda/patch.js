/* global CloudFormationUpdate,Patch */
/**
 * Don't specify a stack name target in the CloudFormationUpdate, since
 * we don't have a predefined stack to target.
 */
CloudFormationUpdate("TereusLambdaExample")({
    "Resources":
    {
        "LambdaTest" :
        {
            "Properties" : Patch.Lambda("./updated_source")
        }
    }
});