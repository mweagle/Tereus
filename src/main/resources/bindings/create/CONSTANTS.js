/* global Java,ArgumentsImpl,UserInfoImpl,Immutable,_ */
// Copyright (c) 2015 Matt Weagle (mweagle@gmail.com)

// Permission is hereby granted, free of charge, to
// any person obtaining a copy of this software and
// associated documentation files (the 'Software'),
// to deal in the Software without restriction,
// including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so,
// subject to the following conditions:

// The above copyright notice and this permission
// notice shall be included in all copies or substantial
// portions of the Software.

// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF
// ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
// SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE


/**
 * <span class="label label-info">Creation Context</span><hr />
 * Global template evaluation Tag values.  Tags
 * are wrapped as an <a href="https://facebook.github.io/immutable-js/">immutable
 * object</a>.  All CloudFormation resources that support
 * <a href="http://docs.aws.amazon.com/search/doc-search.html?searchPath=documentation-guide&searchQuery=tags&x=0&y=0&this_doc_product=AWS+CloudFormation&this_doc_guide=User+Guide&doc_locale=en_us#facet_doc_product=AWS%20CloudFormation&facet_doc_guide=User%20Guide">Tags</a> will be
 * annotated with the supplied TAGS.
 *
 * @example <caption>Accessing TAGS</caption>
 *
 * TAGS.get('myApplicationVersion')
 *
 * @type {Object}
 */
var TAGS = {};


////////////////////////////////////////////////////////////////////////////////
(function initializer() {
    var args = JSON.parse(ArgumentsImpl());
    TAGS = Immutable.Map(args.tags || {});
})();