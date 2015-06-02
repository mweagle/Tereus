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
var Highlight = require('react-highlight');

var templateData = {
  "Description": "Test",
  "Resources": {
    "EC2Instance": {
      "Metadata": {
        "AWS::CloudFormation::Init": {
          "configSets": {
            "default": [
              "configSet0",
              "configSet1"
            ]
          },
          "configSet0": {
            "commands": {
              "test": {
                "command": "echo \"HELLO FROM: $CFNTEST\" &gt; /var/log/test.txt",
                "env": {
                  "CFNTEST": "I come from config1."
                },
                "cwd": "~"
              }
            }
          },
          "configSet1": {
            "commands": {
              "test": {
                "command": "echo \"HELLO FROM: $CFNTEST\" &gt; /var/log/test.txt",
                "env": {
                  "CFNTEST": "I come from config2."
                },
                "cwd": "~"
              }
            }
          }
        }
      },
      "Type": "AWS::EC2::Instance",
      "Properties": {
        "Tags": [
          {
            "Key": "com:tereus:version",
            "Value": "0.1.0"
          }
        ],
        "UserData": {
          "Fn::Join": [
            "",
            [
              "#!/bin/bash -xe\n",
              "yum update -y aws-cfn-bootstrap\n",
              "/opt/aws/bin/cfn-init -v --stack ",
              {
                "Ref": "AWS::StackName"
              },
              " --resource EC2Instance --configsets default --region ",
              {
                "Ref": "AWS::Region"
              },
              "\n"
            ]
          ]
        }
      }
    }
  },
  "Outputs": {},
  "AWSTemplateFormatVersion": "2010-09-09"
};

var EvaluationResults = React.createClass({
  /**
   * @return {object}
   */
  render: function() {
    return (
      <div className="panel panel-default">
  <div className="panel-heading">
    <h3 className="panel-title">Evaluated Template</h3>
  </div>
  <div className="panel-body">
    <Highlight className="json">{JSON.stringify(templateData, null, '  ')}</Highlight>
  </div>
</div>
    );
  }
});

module.exports = EvaluationResults;
