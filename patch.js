/* global CloudFormationUpdate,Patch,ARGUMENTS */
CloudFormationUpdate("SomePatch")({
  "Resources":
  {
    "LambdaTest" :
    {
      "Metadata": {
        "SomeKey": Patch.Add(ARGUMENTS.get("MyKey") || "DefaultArgumentValueInTemplate")
      }
    }
  }
});
