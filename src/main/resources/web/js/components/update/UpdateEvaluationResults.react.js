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
var Highlight = require('react-highlight');
var TereusStore = require('../../stores/TereusStore');
var _ = require('underscore');
var TereusStore = require('../../stores/TereusStore');
var TereusConstants = require('../../constants/TereusConstants');

var EvaluationResults = React.createClass({
  getInitialState: function() {
    var initialAPIState =  TereusStore.getState()[TereusConstants.TEREUS_UPDATE] || {};
    initialAPIState = initialAPIState.outputs || {};
    return initialAPIState;
  },
  onStoreChange: function()
  {
    this.setState(TereusStore.getState()[TereusConstants.TEREUS_UPDATE] || {});
  },
  componentDidMount: function() {
    TereusStore.addChangeListener(this.onStoreChange);
  },
  componentWillUnmount: function() {
    TereusStore.removeChangeListener(this.onStoreChange);
  },
  __prettyFormattedResult: function(resultValue)
  {
    var pretty = '';
    if (resultValue)
    {
      try
      {
        pretty = JSON.stringify(JSON.parse(resultValue), null, ' ');
      }
      catch (e)
      {
        pretty = JSON.stringify({error: e.toString()}, null, ' ');
      }
    }
    return pretty;
  },
  /**
   * @return {object}
   */
  render: function() {
     // If we're processing or there is no state, nothing to show
    var renderState = this.state || {};
    var renderOutputs = renderState.outputs || {};

    if (renderState.processing)
    {
      return (<div className="row">
                <div className="col-md-12">
                  <p className="text-center"><i className="fa fa-5x fa-spinner fa-spin"></i></p>
                </div>
              </div>);
    }
    else if (!_.isEmpty(renderOutputs.error))
    {
      return (<div className="panel panel-danger">
                <div className="panel-heading">Error</div>
                <div className="panel-body">
                  {renderOutputs.error}
                </div>
              </div>)
    }
    else if (renderOutputs.results)
    {
      return (
          <div className="panel panel-default">
            <div className="panel-heading">
              <h3 className="panel-title">Evaluated Template</h3>
            </div>
            <div className="panel-body">
              <div role="tabpanel">
                <ul className="nav nav-tabs" role="tablist">
                  <li role="presentation" className="active"><a href="#evaluated" aria-controls="evaluated" role="tab" data-toggle="tab">JSON Patch</a></li>
                  <li role="presentation"><a href="#template" aria-controls="template" role="tab" data-toggle="tab">Patch Definition</a></li>
                  <li role="presentation"><a href="#targetTemplate" aria-controls="template" role="tab" data-toggle="tab">Target Template</a></li>
                  <li role="presentation"><a href="#transformedTemplate" aria-controls="template" role="tab" data-toggle="tab">Transformed Template</a></li>
                </ul>
                <div className="tab-content">
                  <div role="tabpanel" className="tab-pane active" id="evaluated">
                    <Highlight className="json">{this.__prettyFormattedResult(renderOutputs.results.evaluated)}</Highlight>
                  </div>
                  <div role="tabpanel" className="tab-pane" id="template">
                    <Highlight className="JavaScript">{renderOutputs.results.template}</Highlight>
                  </div>
                  <div role="tabpanel" className="tab-pane" id="targetTemplate">
                    <Highlight className="json">{this.__prettyFormattedResult(renderOutputs.results.target)}</Highlight>
                  </div>
                  <div role="tabpanel" className="tab-pane" id="transformedTemplate">
                    <Highlight className="json">{this.__prettyFormattedResult(renderOutputs.results.applied)}</Highlight>
                  </div>
                </div>
              </div>
            </div>
          </div>);
    }
    else
    {
      return (<div> </div>);
    }
  }
});

module.exports = EvaluationResults;
