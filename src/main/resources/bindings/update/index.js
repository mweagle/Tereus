/* global annotateTemplate,_,logger,__patchTunnel,ArgumentsImpl,TemplateInfoImpl,jsonpatch,Immutable,TAGS*/

// Copyright (c) 2015 Matt Weagle (mweagle@gmail.com)

// Permission is hereby granted, free of charge, to
// any person obtaining a copy of this software and
// associated documentation files (the "Software"),
// to deal in the Software without restriction,
// including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so,
// subject to the following conditions:

// The above copyright notice and this permission
// notice shall be included in all copies or substantial
// portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
// ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
// SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE

/**
 * <span class="label label-info">Update Context</span><hr />
 * Global patch evaluation arguments.  Arguments
 * are wrapped as an <a href="https://facebook.github.io/immutable-js/">immutable
 * object</a>.
 *
 * @example <caption>Accessing ARGUMENTS</caption>
 *
 * ARGUMENTS.get('someCommandLineFlag')
 *
 * @type {Object}
 */
var ARGUMENTS = {};

////////////////////////////////////////////////////////////////////////////////
(function initializer() {
    var args = JSON.parse(ArgumentsImpl());
    ARGUMENTS = Immutable.Map(args.arguments || {});
})();

var CurriedPatch = function(pathlessOp)
{
  return function(pathSpec)
  {
    if (null === pathlessOp.path)
    {
      pathlessOp.path = pathSpec;
    }
    return pathlessOp;
  };
};

/**
<span class="label label-info">Update Context</span><hr />

Encapsulates functions that facilitate creating <a href="http://tools.ietf.org/html/rfc6902">JSON Patch</a> documents.
The object's nested properties become the implicit <a href="http://tools.ietf.org/html/rfc6901">JSON Pointer</a>
_path_ value for most operations.

@namespace Patch
*/
var Patch =
{
 /**
 * Return an <a href="http://tools.ietf.org/html/rfc6902#page-4">add</a> object.  The property key
 * is the implied JSON Pointer in the target document.
 * @param {Object} newValue  - Updated value to use
 */
  Add: function(newValue) {
    return CurriedPatch({
      'op': 'add',
      'path': null,
      'value': newValue
    });
  },
 /**
 * Return a <a href="http://tools.ietf.org/html/rfc6902#page-6">remove</a> object. The property key
 * is the implied JSON Pointer in the target document.
 */
  Remove: function()
  {
    return CurriedPatch({
      'op': 'remove',
      'path': null
    });
  },
 /**
 * Return a <a href="http://tools.ietf.org/html/rfc6902#page-6">replace</a> object. The property key
 * is the implied JSON Pointer in the target document.
 * @param {Object} newValue  - Updated value to use
 */
  Replace: function(newValue)
  {
    return CurriedPatch({
      'op': 'replace',
      'path': null,
      'value' : newValue
    });
  },
 /**
 * Return a <a href="http://tools.ietf.org/html/rfc6902#page-6">move</a> object. The property key
 * is the implied JSON Pointer in the target document.
 * @param {String} someOtherPathSpecifier  - New JSON Pointer path to use for value
 */
  Move: function(someOtherPathSpecifier)
  {
    return CurriedPatch({
      'op': 'move',
      'path': null,
      'from' : someOtherPathSpecifier
    });
  },
 /**
 * Return a <a href="http://tools.ietf.org/html/rfc6902#page-7">copy</a> object. The property key
 * is the implied JSON Pointer in the target document.
 * @param {String} someOtherPathSpecifier  - New JSON Pointer path to use for duplicated value
 */
  Copy: function(someOtherPathSpecifier)
  {
    return CurriedPatch({
      'op': 'copy',
      'path': null,
      'from' : someOtherPathSpecifier
    });
  },
 /**
 * Return a <a href="http://tools.ietf.org/html/rfc6902#page-7">test</a> object. The property key
 * is the implied JSON Pointer in the target document.
 * @param {Object} verifyValue  - Value to verify as part of patch application
 */
  Test: function(verifyValue)
  {
    return CurriedPatch({
      'op': 'test',
      'path': null,
      'value' : verifyValue
    });
  },
  /**
   * Customized Lambda Patch function that handles updating a Lambda function
   *
   @example <caption>Patch.Lambda</caption>
   CloudFormationUpdate("LambdaUpdate")({
    "Resources":
    {
        "LambdaTest" :
        {
            "Properties" : Patch.Lambda("./updated_source")
        }
    }
    });

   * @param {String} newSourceLocation  Relative/absolute path to updated lambda source
   * @param {String} optionalNewHandlerName Optional new lambda handler entrypoint. See the
   *                  <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-lambda-function.html#cfn-lambda-function-handler">Lambda docs</a>
   *                  for supported formats.
   */
  Lambda: function (newSourceLocation, optionalNewHandlerName) {
      return function (pathSpec) {
          // Ensure that the target exists for the JSON pointer
          var lambdaPatch = [];
          lambdaPatch.push({
              'op': 'replace',
              'path': pathSpec + '/Code',
              'value': newSourceLocation
          });
          if (optionalNewHandlerName) {
              lambdaPatch.push({
                  'op': 'replace',
                  'path': pathSpec + '/Handler',
                  'value': optionalNewHandlerName
              });
          }
          return lambdaPatch;
      };
  }
};

/**
 * <span class="label label-info">Update Context</span><hr />
 *
 * The global CloudFormationUpdate function responsible
 * for expanding the inline patch definition.  Note that intermediate
 * JSON Pointer path components will be created iff the
 * target is the subject of an <i>add</i> operation.
 * <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a>
 * <i>path</i> components that did not exist in the target document.
 *
 * @example <caption>CloudFormationUpdate</caption>
  CloudFormationUpdate("TargetCloudFormationStackName")({
    "Resources":
    {
      "MyEc2" : Patch.Add("Foobar")
    }
  });
 *
 *
 * @param {string} stackName - Target Name for patch.  May be null/empty if
 * StackName doesn't yet exist
 */
var CloudFormationUpdate = function (stackName) {
    var stackInfo = _.isEmpty(stackName) ?
                    {} :
                    JSON.parse(TargetStackInfoImpl(stackName));


    TAGS = Immutable.Map(stackInfo.tags || {});
    PARAMS = Immutable.Map(stackInfo.params || {});


    var visitingPatchAccumulator = function (patchRootItem, targetRoot, pathSpec, accumulator) {
        accumulator = accumulator || [];
        for (var eachProp in patchRootItem) {
            // Ensure there is a target while we're doing this...
            var targetChildRoot = targetRoot[eachProp] || {};
            targetRoot[eachProp] = targetChildRoot;

            var eachValue = patchRootItem[eachProp];
            var descendentPath = pathSpec + '/' + eachProp;
            if (typeof(eachValue) === 'function') {
                [].concat(eachValue(descendentPath, targetChildRoot)).forEach(function (eachElement) {
                    accumulator.push(eachElement);
                });
            }
            else if (typeof(eachValue) === 'object') {
                visitingPatchAccumulator(eachValue, targetChildRoot, descendentPath, accumulator);
            }
        }
        return accumulator;
    };

    return function (patchSpec) {
        __patchTunnel.patchTarget = stackInfo.template || '{}';
        __patchTunnel.stackName = stackName;

        var parsedTemplate = JSON.parse(__patchTunnel.patchTarget);

        // Cache whether we need to apply the generation phase...
        var applyAnnotation = !_.isEmpty(parsedTemplate);

        // Evaluate
        var expanded = visitingPatchAccumulator(patchSpec, parsedTemplate, '');

        // Conditionally apply the patch if we have a template definition
        __patchTunnel.patchContents = JSON.stringify(expanded);
        __patchTunnel.appliedResult = '';

        // If there is a patch target, then update it
        if (applyAnnotation) {
            try {
                this.logger.debug('Defined patch: ' + JSON.stringify(expanded, null, ' '));

                var observer = jsonpatch.observe(parsedTemplate);

                // Apply the patch s.t. intermediate values are updated
                jsonpatch.apply(parsedTemplate, expanded);

                // Annotate the template
                parsedTemplate = annotateTemplate(parsedTemplate, PARAMS.toObject(), TAGS.toObject());

                // Generate the computed patch
                var generatedPatch = jsonpatch.generate(observer);

                this.logger.debug('Generated patch: ' + JSON.stringify(generatedPatch, null, ' '));

                // Update the patch contents
                __patchTunnel.patchContents = JSON.stringify(generatedPatch);
                __patchTunnel.appliedResult = JSON.stringify(parsedTemplate);
            }
            catch (ex) {
                throw new Error('Failed to apply patch: ' + ex.toString());
            }
        }
        else {
            __patchTunnel.appliedResult = '';
        }
    };
};
