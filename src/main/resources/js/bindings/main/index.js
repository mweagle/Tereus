/* global load,FileUtils,logger */
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
var tappedLoad = load;

/**
 * Tapped Nashorn <code>load</code> function that falls back to definition
 * scoped relative paths
 * @param  {string} pathArg Resolvable reference, or path relative to definition's
 *                          parent directory
 * @return {undefined}      Undefined - content is `eval`d in current execution context.
 */
var load = function(pathArg)
{
    // Find the first useful thing
    var candidates = [pathArg, FileUtils.resolvedPath(pathArg)];
    var loaded = false;
    for (var i = 0; i !== candidates.length && !loaded; ++i)
    {
      var resourcePath = candidates[i];
      try
      {
        tappedLoad(resourcePath);
        logger.debug('Loaded resolved path: ' + resourcePath);
        loaded = true;
      }
      catch (e)
      {
    	  // NOP
      }
    }
};