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
var BrowserStorage = require('browser-storage');
var async = require('async');
var _ = require('underscore');
var AppDispatcher = require('../dispatcher/AppDispatcher');
var TereusConstants = require('../constants/TereusConstants');
var http = require('http');

var CHANGE_EVENT = 'change';

var isSuccessfulResponse = function(response)
{
  return (response &&
      _.isNumber(response.statusCode) &&
      response.statusCode >= 200 &&
            response.statusCode <= 299);
};

var localCacheKeyname = function(/*parts*/)
{
  var keyname = Array.prototype.slice.call(arguments, 0);
  return (['io.mweagle.tereus'].concat('inputs', keyname)).join('.');
};

var LocalCache = {};
LocalCache.get = function(keyname, defaultValue)
{
  var cached = BrowserStorage.getItem(localCacheKeyname(keyname));
  if (cached)
  {
    try
    {
      cached = JSON.parse(cached);
    }
    catch (e)
    {

    }
  }
  else
  {
    cached = defaultValue || '';
  }
  return cached;
};

LocalCache.set = function(keyname, value)
{
  var nsKeyname = localCacheKeyname(keyname);
  BrowserStorage.setItem(nsKeyname, JSON.stringify(value));
};

////////////////////////////////////////////////////////////////////////////////
// TereusState
////////////////////////////////////////////////////////////////////////////////
var TereusState = {};

// Map of URL paths to their potentiall cached values
//   ARGUMENTS: ['path', 'stackName', 'paramsAndTags', 'region'],
var API = {
  'create' : ['path', 'stackName', 'paramsAndTags', 'region'],
  'update' : ['path', 'stackName', 'arguments', 'region']
};

_.each(API, function(requestKeys, apiName) {
  TereusState[apiName] = {};
  TereusState[apiName].inputs = _.reduce(requestKeys,
                                        function(memo, eachKeyname)
                                        {
                                          var keyname = [apiName, eachKeyname].join('.');
                                          memo[eachKeyname] = LocalCache.get(keyname, null);
                                          return memo;
                                        },
                                        {});
  TereusState[apiName].outputs = {
  };
});

////////////////////////////////////////////////////////////////////////////////
// TereusStore
////////////////////////////////////////////////////////////////////////////////
var TereusStore = assign({}, EventEmitter.prototype, {
  workQueue : async.queue(function (action, callback) {
    var actionHandler = TereusStore.actionHandlers[action.api];
    if (_.isFunction(actionHandler))
    {
      var onResult = function(error, results)
      {
        TereusState[action.api].outputs.error = error ? error.toString() : null;
        TereusState[action.api].outputs.results = error ? null : results;
        TereusState[action.api].processing = false;
        TereusStore.emitChange.call(TereusStore);
        callback(null, null);
      };
      var propertyBag = _.omit(action, 'actionType');
      TereusState[action.api].processing = true;
      TereusState[action.api].outputs = {};
      TereusStore.emitChange.call(TereusStore);
      actionHandler.call(TereusStore, propertyBag, onResult);
    }
  }, 1),
  getState: function() {
    return TereusState;
  },

  emitChange: function() {
    this.emit(CHANGE_EVENT);
  },
  /**
   * @param {function} callback
   */
  addChangeListener: function(callback) {
    this.on(CHANGE_EVENT, callback);
  },

  /**
   * @param {function} callback
   */
  removeChangeListener: function(callback) {
    this.removeListener(CHANGE_EVENT, callback);
  },

  // These will be added in a bit
  actionHandlers : {}

});

TereusStore.__apiCall = function(pathComponent, jsonRequest, callback)
{
  // Clear the current state
  this.emitChange();

  var apiOptions = {
    method: 'POST',
    path: '/api/' + pathComponent,
    headers: {}
  };
  var body = jsonRequest ? JSON.stringify(jsonRequest) : null;
  if (jsonRequest)
  {
    apiOptions.headers['Content-Type'] = 'application/json';
    apiOptions.headers['Content-Length'] = Buffer.byteLength(body, 'utf-8');

    // Save the state
    _.each(jsonRequest, function (eachValue, eachKey) {
      var nsKey = [pathComponent, eachKey].join('.');
      LocalCache.set(nsKey, eachValue);
    });
  }
  var terminus = function(error, response)
  {
    if (!error && !isSuccessfulResponse(response))
    {
        error = new Error(response.body);
    }
    else if (response)
    {
      try
      {
        response = JSON.parse(response.body);
      }
      catch (e)
      {
        error = e.toString();
      }
    }
    callback(error, response);
  };

  var req = http.request(apiOptions);
  req.on('response', function (response)
  {
    if (_.isNumber(response.statusCode))
    {
      if (0 === response.statusCode)
      {
        // Network error, it'll be turned into an error event
      }
      else
      {
        // Consume the response...
        var data = '';
        response.on('data', function (chunk) {
          data += chunk;
        });
        response.on('end', function(){
          response.body = data;
          terminus(null, response);
        });
      }
    }
    else
    {
      terminus(new Error('Unknown response'));
    }
  });
  req.on('error', function (e)
  {
    terminus(e, null);
  });
  req.end(body || null);
};

// Register the action handlers
TereusStore.actionHandlers[TereusConstants.TEREUS_CREATE] = _.bind(_.partial(TereusStore.__apiCall, TereusConstants.TEREUS_CREATE), TereusStore);
TereusStore.actionHandlers[TereusConstants.TEREUS_UPDATE] = _.bind(_.partial(TereusStore.__apiCall, TereusConstants.TEREUS_UPDATE), TereusStore);

////////////////////////////////////////////////////////////////////////////////
// Register action handlers with the Dispatcher
////////////////////////////////////////////////////////////////////////////////
AppDispatcher.register(function(action) {
  TereusStore.workQueue.push(action);
});
module.exports = TereusStore;
