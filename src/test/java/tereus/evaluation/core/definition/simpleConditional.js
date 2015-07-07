CloudFormationTemplate("Test")({
    "Description": "Test",
    "Resources": {
        "Hello": "World",
        "Conditional" : function()
        {
        	if (TAGS.get('foo') === 'undefined')
    		{
        		return {
        			"Properties" : "Will be omitted"
        		}
    		}
        }
    },
    "Outputs": {}
});