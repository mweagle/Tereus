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


@namespace Patch
*/

var Patch =
{
  /**
 * Embed an external file
 * @param {String} pathArg  - Path, relative to template directory root, to embed
 */
  Add: function(newValue) {
    return CurriedPatch({
      'op': 'add',
      'path': null,
      'value': newValue
    });
  },
  Remove: function()
  {
    return CurriedPatch({
      'op': 'remove',
      'path': null
    });
  },
  Replace: function(newValue)
  {
    return CurriedPatch({
      'op': 'replace',
      'path': null,
      'value' : newValue
    });
  },
  Move: function(someOtherPathSpecifier)
  {
    return CurriedPatch({
      'op': 'move',
      'path': null,
      'from' : someOtherPathSpecifier
    });
  },
  Copy: function(someOtherPathSpecifier)
  {
    return CurriedPatch({
      'op': 'copy',
      'path': null,
      'from' : someOtherPathSpecifier
    });
  },
  Test: function(verifyValue)
  {
    return CurriedPatch({
      'op': 'test',
      'path': null,
      'value' : verifyValue
    });
  }
};

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

    // Conditionally apply the patch if we have a template definition
    if (TemplateInfoImpl)
    {
      __patchTunnel.patchTarget = JSON.stringify(TemplateInfoImpl.getTemplateBody());
      var parsedTemplate = JSON.parse(TemplateInfoImpl.getTemplateBody());
      __patchTunnel.appliedResult = JSON.parse(JSON8Patch.apply(parsedTemplate, expanded));
    }
    else
    {
      __patchTunnel.patchTarget = '';
      __patchTunnel.appliedResult = '';
    }
  };
};
