/* global AWS,_*/

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
<span class="label label-info">Creation Context</span><hr />

Encapsulates functions that are convenience builders for creating
<a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-lambda-function.html">Lambda</a>
CloudFormation resources.

@namespace AWS.Lambda
*/
AWS.Lambda = {
    /**
     * Return an <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-lambda-function.html#cfn-lambda-function-code">
     * AWS::Lambda::Function</a>
     * object for CloudFormation.  Providing a template-directory relative string value
     * for the <code>Code</code> will trigger an automatic:
     *    <ol>
     *     <li><b>npm install</b> (if <i>package.json</i> detected)</li>
     *     <li>ZIPping of the resulting source archive</li>
     *     <li>Automatic upload of the Zipfile to S3 for referencing</li>
     *    </ol>
     * The standard <a href="http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-lambda-function-code.html">Code</a>
     * object is also supported and no additional upload is performed.  Note that
     * uploads are based on template-relative <code>Code</code> definitions.  Multiple
     * Lambda resources based on the same source directory will <b>all</b> use the same
     * ZIP archive.
     *
     * <b>Note</b>: Including an <code>S3Key</code> property as part of the
     * <code>additionalUserProps</code> defines a stable identifier for the Lambda
     * payload upload.  If this key is not defined, Tereus will use an automatically
     * generated S3 keyname.
     *
     An illustration of how to provision a Lambda resource is below.

     * @example <caption><i>definition.js</i></caption>

     "LambdaTest" : AWS.Lambda.Function(
     {
          "Code" : "./resources/lambdaSourceDirectory",
          "Description" : "Serverless is the future",
          "Handler" : "index.handler"
     })

     * @example <caption><i>./resources/lambdaSourceDirectory/index.js</i></caption>

    ////////////////////////////////////
    // File: index.js
    console.log('Loading function');

    exports.handler = function(event, context) {
        console.log('value1 =', event.key1);
        console.log('value2 =', event.key2);
        console.log('value3 =', event.key3);
        context.succeed(event.key1);  // Echo back the first key value
        // context.fail('Something went wrong');
    };
     *
     * @param {Object} additionalUserProps  - Additional user properties to compose with defaults.
     */
  Function: function (additionalUserProps)
  {
    var properties = _.extend({
      Runtime: 'nodejs'
    }, additionalUserProps);
    return {
      'Type': 'AWS::Lambda::Function',
      'Properties': properties
    };
  }
};