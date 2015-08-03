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

var React = require('react');
var ReactPropTypes = React.PropTypes;
var CreateArgumentsView = require('./create/CreateArgumentsView.react');
var CreateEvaluationResults = require('./create/CreateEvaluationResults.react');
var UpdateArgumentsView = require('./update/UpdateArgumentsView.react');
var UpdateEvaluationResults = require('./update/UpdateEvaluationResults.react');
var TereusConstants = require('../constants/TereusConstants');
var TereusStore = require('../stores/TereusStore');

var MainView = React.createClass({
  /**
   * @return {object}
   */
  render: function() {
    return (
      <div>
        <ul className="nav nav-tabs" role="tablist">
          <li role="presentation" className="active"><a href="#create" aria-controls="create" role="tab" data-toggle="tab">Create</a></li>
          <li role="presentation"><a href="#update" aria-controls="update" role="tab" data-toggle="tab">Update</a></li>
        </ul>

        <div className="tab-content">
          <div role="tabpanel" className="tab-pane active" id="create">
            <CreateArgumentsView />
            <CreateEvaluationResults />
          </div>
          <div role="tabpanel" className="tab-pane" id="update">
            <UpdateArgumentsView />
            <UpdateEvaluationResults />
          </div>
        </div>
      </div>
  )}
});

module.exports = MainView;
