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
var TereusActions = require('../../actions/TereusActions');
var AWSRegionSelector = require('../common/AWSRegionSelector.react');
var TereusStore = require('../../stores/TereusStore');
var TereusConstants = require('../../constants/TereusConstants');

var ArgumentsView = React.createClass({
  getInitialState: function() {
    var initialAPIState =  TereusStore.getState()[TereusConstants.TEREUS_CREATE] || {};
    initialAPIState = initialAPIState.inputs || {};
    initialAPIState.region = initialAPIState.region || 'us-east-1';
    initialAPIState.paramsAndTags = initialAPIState.paramsAndTags ||
                                            {
                                              Parameters: {
                                                BucketName : 'S3 BUCKET NAME'
                                              },
                                              Tags: {}
                                            };
    initialAPIState.isParamsAndTagsValid = true;
    return initialAPIState;
  },
  onEvaluate: function(event) {
    TereusActions.create(this.state.path, this.state.region, this.state.paramsAndTags);
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
    var selectedRegion = this.state.region;
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
                      <input type="string" className="form-control input-sm" id="inputPath" placeholder="Path" defaultValue={this.state.path} onChange={this.onStateChange('path')}></input>
                        <span className="input-group-btn">
                          <button className="btn btn-default btn-sm btn-primary" type="button" onClick={this.onEvaluate}>Evaluate</button>
                        </span>
                    </div>
                  </div>
              </div>
            </div>

            <div className="row">
              <div className="col-md-6">
                  <AWSRegionSelector defaultRegion={this.state.region} onChange={this.onStateChange('region')} />
              </div>
              <div className="col-md-6">
                <div className={paramsAndTagsClasses} ref="paramsAndTags">
                  <label for="jsonData">Params and Tags (JSON)</label>
                  <textarea id="jsonData" ref="jsonData" className="form-control"
                  defaultValue={JSON.stringify(this.state.paramsAndTags, null, ' ')}
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