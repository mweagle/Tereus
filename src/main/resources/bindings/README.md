# Overview

Documentation of the Tereus CloudFormation template evaluation environment.

## Summary

Tereus is a tool that enables you to compose and provision AWS CloudFormation resources
using JavaScript.  Rather than externally validating and linking JSON fragments or
representing components using an alternative syntax (eg, Markdown, YAML, etc.),
you can create dynamic, expressive templates for different environments using
JavaScript.

The Tereus evaluation context is opt-in and user-extensible to suit your own workflow
requirements.  The simplest Tereus-compatible template definition is just an object
definition passed to the `CloudFormationTemplate` function.  For example:

```
CloudFormationTemplate("Test")({
    "Description": "Test",
    "Resources": {
        "SomeResource" :{
          "Type" : "AWS::....",
          "Properties" :
          {

          }
        }
    },
    "Outputs": {}
});
```

The `CloudFormationTemplate` function accepts a single argument (the _Stack Name_) and
returns a function whose single argument is a JSON-object representing a CloudFormation
template.

The evaluation context includes a set of global functions and namespaces to assist constructing
CloudFormation templates.  For instance:

```
  "SomeProperty": Embed.File("resources/embeddable.txt")
```

The `Embed` namespace exposes functions that automatically parse external content
into [Fn::Join](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-join.html) compatible representations.  See the full [Embed docs](Embed.html) for
more information.

## Included Libraries

In addition to Tereus-specific extensions, the evaluation context also preloads the following
utility libraries:

  - [Underscore JS](http://underscorejs.org/)
  - [Immutable JS](https://facebook.github.io/immutable-js/)

## Extending the evaluation context

It is also possible to extend the evaluation context with your own custom
JavaScript functions via the `load` function.  For instance:


```
////////////////////////////////////
// helpers.js
var provideSomeData = function()
{
  return "Hello World";
}
```

```
////////////////////////////////////
// main.js
load('helpers.js');

CloudFormationTemplate("Test")({
    "Description": "Test",
     "Resources": {
       "SomeResource" : {
         "Metadata":
         {
           "ExtraData" : provideSomeData()
         }
       }
     },
     "Outputs": {}
});

```

## Accessing Java APIs

Finally, you can also call Java code directly inside
the JS evaluation context using *Java.type*.  For instance,

```
  var JavaPath = Java.type("java.nio.file.Paths");
  var myFilepath = JavaPath.get(FileUtils.resolvedPath('foo.txt'));
```

## Tips

### Conditional Resources

Complex environments may conditionally require AWS resources.  For example, you
may create per-developer SQS resources in *Dev*, but both *Stage* and
*Production* should use pre-existing global resources.

This can be expressed by returning `undefined` from a function
that defines a resource.  For instance:

```
  "DevOnlyResource" : function()
  {
    if (TAGS.get('Target') === 'Dev')
    {
      return {
        "Properties" :{
          // ...
        }
      };
    }
    else
    {
      // Only create the resource in Development
      return undefined;
    }
  }
```

The `undefined` property value will be omitted from the fully evaluated
template.