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
var TereusStore = require('../stores/TereusStore');
var _ = require('underscore');

var isOutputAvailable = function(evaluationState)
{
  return (!_.isEmpty(evaluationState) &&
          !_.isEmpty(evaluationState.outputs));
}
var EvaluationResults = React.createClass({
  propTypes: {
    arguments: ReactPropTypes.object.isRequired
  },
  onStoreChange: function()
  {
    // TODO - update proper keys
    this.setState(TereusStore.getState());
  },
  componentDidMount: function() {
    TereusStore.addChangeListener(this.onStoreChange);
  },

  componentWillUnmount: function() {
    TereusStore.removeChangeListener(this.onStoreChange);
  },

  /**
   * @return {object}
   */
  render: function() {

    if (this.state && this.state.processing)
    {
      return (<div className="row">
                <div className="col-md-12">
                <p className="text-center"><i className="fa fa-5x fa-spinner fa-spin"></i></p>
                </div>
              </div>);
    }
    else if (isOutputAvailable(this.state))
    {
      if (!_.isEmpty(this.state.outputs.error))
      {
        return (<div className="panel panel-danger">
                  <div className="panel-heading">Error</div>
                  <div className="panel-body">
                    {this.state.outputs.error}
                  </div>
                </div>)
      }
      else
      {
        return (
          <div className="panel panel-default">
            <div className="panel-heading">
              <h3 className="panel-title">Evaluated Template</h3>
            </div>
            <div className="panel-body">
              <div role="tabpanel">
                <ul className="nav nav-tabs" role="tablist">
                  <li role="presentation" className="active"><a href="#evaluated" aria-controls="evaluated" role="tab" data-toggle="tab">Evaluated</a></li>
                  <li role="presentation"><a href="#template" aria-controls="template" role="tab" data-toggle="tab">Raw Template</a></li>
                </ul>
                <div className="tab-content">
                  <div role="tabpanel" className="tab-pane active" id="evaluated">
                      <Highlight className="json">{JSON.stringify(this.state.outputs.results.evaluated, null, ' ')}</Highlight>
                  </div>
                  <div role="tabpanel" className="tab-pane" id="template">
                    <Highlight className="no-highlight">{this.state.outputs.results.template}</Highlight>
                  </div>
                </div>
              </div>
            </div>
          </div>);
      }
    }
    else
    {
      return (<div />);
    }
  }
});

module.exports = EvaluationResults;
