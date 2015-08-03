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

var AWSRegionSelector = React.createClass({
  propTypes: {
    defaultRegion: ReactPropTypes.string,
    onChange: React.PropTypes.func.isRequired
  },

  render: function() {
    return(
      <div className="form-group">
        <label for="inputRegion">AWS Region</label>
        <select defaultValue={this.props.defaultRegion} className="form-control input-sm" onChange={this.props.onChange}>{REGION_MARKUP}</select>
      </div>
      )
  }
});

module.exports = AWSRegionSelector;
