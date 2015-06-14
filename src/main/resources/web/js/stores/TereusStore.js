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

var CONSTANTS = {
  ARGUMENTS: ['path', 'stackName', 'paramsAndTags', 'region'],
  DEFAULTS: {
    paramsAndTags: {
      Parameters: {
        BucketName : 'S3 BUCKET NAME'
      },
      Tags: {}
    },
    region: 'us-east-1'
  }
};

var API_EVALUATOR_OPTIONS = {
  method: 'POST',
  path: '/api/evaluator',
  headers: {
    'Content-Type' : 'application/json'
  }
};

var evaluatorRequestOptions = function(requestLength)
{
  var options = _.clone(API_EVALUATOR_OPTIONS);
  if (requestLength)
  {
    options.headers['Content-Length'] = requestLength;
  }
  return options;
};


var localCacheKeyname = function(/*parts*/)
{
  var keyname = Array.prototype.slice.call(arguments, 0);
  return (['io.mweagle.tereus'].concat(keyname)).join('.');
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
TereusState.inputs = _.reduce(CONSTANTS.ARGUMENTS,
                  function (memo, eachArgument)
                  {
                    memo[eachArgument] = LocalCache.get(eachArgument,
                                        CONSTANTS.DEFAULTS[eachArgument]);

                    return memo;
                  },
                  {});
TereusState.outputs = {
  error: null,
  results : ''
};


////////////////////////////////////////////////////////////////////////////////
// TereusStore
////////////////////////////////////////////////////////////////////////////////
var TereusStore = assign({}, EventEmitter.prototype, {
  workQueue : async.queue(function (action, callback) {
    var actionHandler = TereusStore.actionHandlers[action.actionType];
    if (_.isFunction(actionHandler))
    {
      var onResult = function(error, results)
      {
        TereusState.outputs.error = error ? error.toString() : null;
        TereusState.outputs.results = TereusState.outputs.error ? null : results;
        TereusStore.emitChange.call(TereusStore);
        TereusState.processing = false;
        TereusStore.emitChange.call(TereusStore);
        callback(null, null);
      };
      var propertyBag = _.omit(action, 'actionType');
      TereusState.processing = true;
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

// Register the action handlers
TereusStore.actionHandlers[TereusConstants.TEREUS_EVALUATE] = function(propertyBag, callback)
{
  // Clear the current state
  this.emitChange();

  // Post the data to the server and send it back
  var tasks = [];

  tasks[0] = function(requestCB)
  {
    // Save the state
    _.each(propertyBag, function (eachValue, eachKey) {
      LocalCache.set(eachKey, eachValue);
    });
    var body = JSON.stringify(propertyBag);
    var options = evaluatorRequestOptions(Buffer.byteLength(body, 'utf-8'));
    var req = http.request(options);
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
            requestCB(null, response);
        }
      }
    });
    req.on('error', function (e)
    {
      requestCB(e, null);
    });
    req.end(body);
  };

  tasks[1] = function(response, requestCB)
  {
    // Consume the response
    var data = '';
    response.on('data', function (chunk) {
      data += chunk;
    });
    response.on('end', function(){
      response.body = data;
      requestCB(null, response);
    });
  };

  var terminus = function(error, response)
  {
    if (!error && !isSuccessfulResponse(response))
    {
        error = new Error(response.body);
    }
    TereusState.outputs.error = error ? error.toString() : null;
    if (!TereusState.outputs.error && response)
    {
      try
      {
        response = JSON.parse(response.body);

        // The evaluated property is JSON
        response.evaluated = JSON.parse(response.evaluated);
      }
      catch (e)
      {
        error = e.toString();
      }
    }
    callback(error, response);
  };
  async.waterfall(tasks, terminus);
};

////////////////////////////////////////////////////////////////////////////////
// Register action handlers with the Dispatcher
////////////////////////////////////////////////////////////////////////////////
AppDispatcher.register(function(action) {
  TereusStore.workQueue.push(action);
});
module.exports = TereusStore;
