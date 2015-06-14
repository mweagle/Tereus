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
//
var React = require('react');
var ReactPropTypes = React.PropTypes;
var _ = require('underscore');
var classNames = require('classnames');
var TereusActions = require('../actions/TereusActions');

var REGIONS = ['us-east-1',
              'us-west-1',
              'us-west-2',
              'eu-west-1',
              'eu-central-1',
              'ap-southeast-1',
              'ap-southeast-2',
              'ap-northeast-1'];

var REGION_MARKUP = _.map(REGIONS,
                          function (eachRegion)
                          {
                            return <option key={eachRegion}>{eachRegion}</option>;
                          });
/**

TODO - update arguments/inputs to use proper key for paramsAndTags
**/
var ArgumentsView = React.createClass({
  propTypes: {
    arguments: ReactPropTypes.object.isRequired
  },
  getInitialState: function() {
    var initialState =
    {
      isParamsAndTagsValid: true,
    };
    return initialState;
  },
  onEvaluate: function(event) {
    var data = _.extend({},
                        this.props.arguments || {},
                        this.state || {});
    // Gather up all the data, throw it into a giant object.
    TereusActions.evaluate(data.path, data.region, data.stackName, data.paramsAndTags);
  },
  onStateChange: function(stateKeyname) {
    var self = this;
    return function(event) {
      var mergeState = {};
      mergeState[stateKeyname] = event.target.value;

      // If this is the JSON data...validate it
      if (stateKeyname === 'paramsAndTags' && !_.isEmpty(event.target.value))
      {
        try
        {
          var parsed = JSON.parse(event.target.value);
          self.state.isParamsAndTagsValid = true;
          mergeState[stateKeyname] = parsed;
        }
        catch (e)
        {
          self.state.isParamsAndTagsValid = false;
        }
      }
      self.setState(mergeState);
    };
  },
  /**
   * @return {object}
   */
  render: function() {
    var paramsAndTagsClasses = classNames({
      'form-group': true,
      'has-error': !this.state.isParamsAndTagsValid,
      'has-success': this.state.isParamsAndTagsValid
    });
    var selectedRegion = this.state.region || this.props.arguments.region;
    return (
  <div className="panel panel-default">
    <div className="panel-heading">
      <h3 className="panel-title">Inputs</h3>
    </div>
    <div className="panel-body">
      <form>
        <div className="row">
          <div className="col-md-12">
            <div className="form-group">
                <label for="inputPath">Definition Path</label>
                <div className="input-group">
                  <input type="string" className="form-control input-sm" id="inputPath" placeholder="Path" defaultValue={this.props.arguments.path} onChange={this.onStateChange('path')}></input>
                    <span className="input-group-btn">
                      <button className="btn btn-default btn-sm btn-primary" type="button" onClick={this.onEvaluate}>Evaluate</button>
                    </span>
                </div>
              </div>
          </div>
        </div>

        <div className="row">
          <div className="col-md-6">
              <div className="form-group">
                <label for="inputRegion">AWS Region</label>
                <select value={selectedRegion} className="form-control" onChange={this.onStateChange('region')}>{REGION_MARKUP}</select>
              </div>
              <div className="form-group">
                <label for="inputName">Stack Name (optional)</label>
                  <input type="string" className="form-control input-sm" id="inputName" placeholder="Name" defaultValue={this.props.arguments.stackName} onChange={this.onStateChange('stackName')}></input>
              </div>
          </div>
          <div className="col-md-6">
            <div className={paramsAndTagsClasses} ref="paramsAndTags">
              <label for="jsonData">Params and Arguments (JSON)</label>
              <textarea id="jsonData" ref="jsonData" className="form-control"
              defaultValue={JSON.stringify(this.props.arguments.paramsAndTags, null, ' ')}
              rows="8" onChange={this.onStateChange('paramsAndTags')}></textarea>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
    );
  }
});

module.exports = ArgumentsView;