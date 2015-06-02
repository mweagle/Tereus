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

var EventEmitter = require('events').EventEmitter;
var assign = require('object-assign');
var _ = require('underscore');
var LocalStorage = require('local-storage');
var AppDispatcher = require('../dispatcher/AppDispatcher');
var TereusConstants = require('../constants/TereusConstants');

var namespacedConstant = function(/*parts*/)
{
  var keyname = Array.prototype.slice.call(arguments, 0);
  return (['io.mweagle.tereus'].concat(keyname)).join('.');
};

var cachedValues =  function(/*parts*/)
{
  var valueNames = Array.prototype.slice.call(arguments, 0);
  return _.reduce(valueNames,
                  function (memo, keyname)
                  {
                    memo[keyname] = namespacedConstant(keyname);
                    return memo;
                  },
                  {});
};
var CONSTANTS = {
  ARGUMENTS: cachedValues('path', 'stackName', 'paramsAndTags'),
  DEFAULTS: {
    paramsAndTags: {
      Parameters: {},
      Tags: {}
    }
  }
};

var TereusStore = assign({}, EventEmitter.prototype, {
  getArguments: function() {
    return _.reduce(CONSTANTS.ARGUMENTS,
                function (memo, eachLSKey, eachKeyname)
                {
                  var defaultValue = CONSTANTS.DEFAULTS[eachKeyname] || '';
                  memo[eachKeyname] = LocalStorage(eachLSKey) || defaultValue;
                  try
                  {
                    memo[eachKeyname] = JSON.parse(memo[eachKeyname]);
                  }
                  catch (e)
                  {
                    // NOP - maybe it's a number?

                  }
                  return memo;
                },
                {});
  },
  // These will be added in a bit
  actionHandlers : {}

});
// Register the action handlers
TereusStore.actionHandlers[TereusConstants.TEREUS_EVALUATE] = function(propertyBag)
{
  window.alert('BAG:' + JSON.stringify(propertyBag, null, ' '));
};

// Register the dispacher
AppDispatcher.register(function(action) {
  var actionHandler = TereusStore.actionHandlers[action.actionType];
  if (_.isFunction(actionHandler))
  {
    var propertyBag = _.omit(action, 'actionType');
    try
    {
      actionHandler.call(TereusStore, propertyBag);
    }
    catch (e)
    {
      if (console)
      {
        console.error(e.toString());
      }
    }
  }
});
module.exports = TereusStore;
