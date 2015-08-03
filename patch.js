JSONPatch("SomePatch")({
  "Resources":
  {
    "MyEc2" : Patch.Add("Foobar")
  }
})


doc = JSON8Patch.add({}, '/foo', 'foo')[0]
logger.info('TRANSFORMED: ' + JSON.stringify(doc, null,  ' '));