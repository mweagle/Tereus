var PACKAGE_INFO = require('./package.json');
var util = require('util');

exports.handler = function(event, context) {
    console.log('Tereus Lambda event: ' + util.inspect(event, { showHidden: true, depth: null }));
    context.succeed('Hello from Tereus:' + PACKAGE_INFO.version);
};