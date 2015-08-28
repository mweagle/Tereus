/* global CloudFormationUpdate,Patch */
CloudFormationUpdate("SomePatch")({
  "Key":
  {
    "Subkey" : Patch.Add("Value")
  }
});