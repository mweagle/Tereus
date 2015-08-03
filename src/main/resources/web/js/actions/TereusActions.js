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

var AppDispatcher = require('../dispatcher/AppDispatcher');
var TereusConstants = require('../constants/TereusConstants');

var TereusActions = {

  /**
   * Evaluate a template definition
   * @param  {String} templatePath  Path to JS template definition
   * @param  {String} region        AWS region for stack
   * @param  {String} stackName     Optional stack name
   * @param  {Object} paramsAndTags Parameters and Tags object
   * @return {undefined}            Undefined
   */
  create: function(templatePath, region, stackName, paramsAndTags) {
    AppDispatcher.dispatch({
      api: TereusConstants.TEREUS_CREATE,
      path: templatePath,
      stackName: stackName,
      region: region,
      paramsAndTags: paramsAndTags
    });
  },
  update: function(patchPath, region, stackName, updateArgs)
  {
    AppDispatcher.dispatch({
      api: TereusConstants.TEREUS_UPDATE,
      path: patchPath,
      stackName: stackName,
      region: region,
      arguments: updateArgs
    });
  }
};

module.exports = TereusActions;