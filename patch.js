/* global JSONPatch,Patch,ARGUMENTS */
JSONPatch("SomePatch")({
  "Resources":
  {
    "MyEc2" : Patch.Add(ARGUMENTS.get("MyKey") || "DefaultArgumentValueInTemplate")
  }
});