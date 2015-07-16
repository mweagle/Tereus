/* global AWS,CONSTANTS,_ */

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

/**

Encapsulates functions that are convenience builders for creating
<a href="http://docs.aws.amazon.com/search/doc-search.html?searchPath=documentation-guide&searchQuery=EC2&x=0&y=0&this_doc_product=AWS+CloudFormation&this_doc_guide=User+Guide&doc_locale=en_us#facet_doc_product=AWS%20CloudFormation&facet_doc_guide=User%20Guide">EC2</a>
CloudFormation resources.

@namespace AWS.EC2
*/
AWS.EC2 = {
    /**
     * Return default EC2 properties object required to create
     * <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-instance.html">AWS::EC2::Instance</a>
     *
     * @param {Object} additionalUserProps  - Additional user properties to compose with defaults.
     */
    WithProperties: function (additionalUserProps) {
        return _.extend({
            'InstanceType': {
                'Ref': CONSTANTS.PARAMETERS.KEYNAMES.INSTANCE_TYPE
            },
            'KeyName': {
                'Ref': CONSTANTS.PARAMETERS.KEYNAMES.SSH_KEY_NAME
            },
            'ImageId': {
                'Fn::FindInMap': [CONSTANTS.MAPPINGS.KEYNAMES.REGION_ARCH_2_AMI, {
                    'Ref': 'AWS::Region'
                }, {
                    'Fn::FindInMap': [CONSTANTS.MAPPINGS.KEYNAMES.INSTANCE_TYPE_2_ARCH, {
                        'Ref': CONSTANTS.PARAMETERS.KEYNAMES.INSTANCE_TYPE
                    }, 'Arch']
                }]
            }
        }, additionalUserProps || {});
    }
};