/* global __patchTunnel,ArgumentsImpl,TemplateInfoImpl,JSON8Patch,Immutable,_ */

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

Encapsulates functions that facilitate creating [JSON Patch](http://tools.ietf.org/html/rfc6902) documents.
The object's nested properties become the implicit [JSON Pointer](https://tools.ietf.org/html/rfc6901)
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
  }
};

/**
 * <span class="label label-info">Update Context</span><hr />
 *
 * The global JSONPatch function responsible
 * for expanding the inline patch definition.
 *
 * @example <caption>JSONPatch</caption>
  JSONPatch("SomePatch")({
    "Resources":
    {
      "MyEc2" : Patch.Add("Foobar")
    }
  });
 *
 *
 * @param {string} patchName - Patch Name
 */
var JSONPatch = function(patchName)
{
  var visitingPatchAccumulator = function(rootItem, pathSpec, accumulator)
  {
    accumulator = accumulator || [];
    for (var eachProp in rootItem)
    {
      var eachValue = rootItem[eachProp];
      var descendentPath = pathSpec + '/' + eachProp;
      if (typeof(eachValue) === 'function')
      {
        accumulator.push(eachValue(descendentPath));
      }
      else if (typeof(eachValue) === 'object')
      {
        visitingPatchAccumulator(eachValue, descendentPath, accumulator);
      }
    }
    return accumulator;
  };

  return function(patchSpec)
  {
    var expanded = visitingPatchAccumulator(patchSpec, '');
    // Put the result into something we can get at
    __patchTunnel.patchName = patchName;
    __patchTunnel.patchContents = JSON.stringify(expanded);

    // Conditional  ly apply the patch if we have a template definition
    __patchTunnel.patchTarget = '';
    __patchTunnel.appliedResult = '';
    try
    {
      __patchTunnel.patchTarget = JSON.stringify(TemplateInfoImpl.getTemplateBody());
      var parsedTemplate = JSON.parse(TemplateInfoImpl.getTemplateBody());
      __patchTunnel.appliedResult = JSON.parse(JSON8Patch.apply(parsedTemplate, expanded));
    }
    catch (ex)
    {
      // NOP
    }
  };
};
