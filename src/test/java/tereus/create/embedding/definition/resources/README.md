You can include ERB style delimiters
for pre-execution time values: <%= value %>
=====

And you can add TAG values! See <%= TAGS.get("SomeTag") %>

AWS Expressions in Mustache-style delimters have access
to AWS-specific expansion as in:
=====

Notes for  {{ "Fn::GetAtt" : ["MyThing","OtherThing"] }} 

And another entry {{"Ref" : "AWS::Region"}}
