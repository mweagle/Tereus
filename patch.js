/* global JSONPatch,Patch */
JSONPatch("SomePatch")({
  "Resources":
  {
    "MyEc2" : Patch.Add("Foobar")
  }
});