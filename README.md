# Overview

Tereus is a tool that simplifies defining, composing, and creating AWS
[CloudFormation](http://aws.amazon.com/cloudformation/) templates.  Template
definitions can start as simple JavaScript object definitions and be
extended using built-in and user-defined JavaScript functions.

# Prerequisites

To build Tereus, ensure you have:

    * Java 1.8
    * [Maven](https://maven.apache.org/)
    * [Gradle](https://gradle.org/)

And run:

```
gradle build

```

This will generate an uber jar _build/libs/Tereus.jar_ which includes
all necessary runtime dependencies.

# Usage

Tereus supports two modes of operation:
    * Command Line
    * Web Interface

## Command Line

The command line interface is appropriate as part of a larger automated build.  The command line is also the _only_ way to actually provision a CloudFormation stack
following a successful evaluation.

Additional documentation is available via `java -jav Tereus.jar`.  Sample CLI output below:

```
12:54 $ java -jar build/libs/Tereus.jar
12:55:09.986 [main] ERROR com.mweagle.Tereus - java.lang.IllegalArgumentException: Please provide a stack definition path
NAME
        Tereus - Evaluates a CloudFormation template expressed as a function

SYNOPSIS
        Tereus
                [(-a <jsonParamAndTagsPath> | --arguments <jsonParamAndTagsPath>)]
                [(-b <s3BucketName> | --bucket <s3BucketName>)] [(-h | --help)]
                [(-i | --gui)] [(-n | --noop)]
                [(-o <outputFilePath> | --output <outputFilePath>)]
                [(-p <port> | --port <port>)] [(-r <region> | --region <region>)]
                [(-s <stackName> | --stack <stackName>)]
                [(-t <stackTemplatePath> | --template <stackTemplatePath>)]

OPTIONS
        -a <jsonParamAndTagsPath>, --arguments <jsonParamAndTagsPath>
            Path to JSON file including "Parameters" & "Tags" values

        -b <s3BucketName>, --bucket <s3BucketName>
            [REQUIRED] S3 Bucketname to host template content

        -h, --help
            Display help information

        -i, --gui
            [OPTIONAL] Start the UI

        -n, --noop
            Dry run - stack will NOT be created (default=true)

        -o <outputFilePath>, --output <outputFilePath>
            Optional file to which evaluated template will be saved

        -p <port>, --port <port>
            [OPTIONAL] Alternative port for UI HTTP server

        -r <region>, --region <region>
            AWS Region (default=us-east-1)

        -s <stackName>, --stack <stackName>
            Optional Stack Name to use. If empty,
            {basename+SHA256(templateData)} will be provided

        -t <stackTemplatePath>, --template <stackTemplatePath>
            [REQUIRED] Path to CloudFormation definition
```

## Web Interface

Tereus also supports a Web-based UI to support interactive usage.  The UI is available by providing the `-i/--gui` command line option as in:

```
java -jar build/libs/Tereus.jar --gui
12:55 $ java -jar build/libs/Tereus.jar --gui
[Thread-0] INFO spark.webserver.SparkServer - == Spark has ignited ...
[Thread-0] INFO spark.webserver.SparkServer - >> Listening on 0.0.0.0:4567
[Thread-0] INFO org.eclipse.jetty.server.Server - jetty-9.0.z-SNAPSHOT
[Thread-0] INFO org.eclipse.jetty.server.ServerConnector - Started ServerConnector@507d0e15{HTTP/1.1}{0.0.0.0:4567}
12:58:15.071 [main] INFO  com.mweagle.Tereus - Tereus UI available at http://localhost:4567/
```

And visiting [http://localhost:4567/](http://localhost:4567/).  (The port can be overriden using the `-p/--port` CLI option.)

The UI is served by [Spark](http://sparkjava.com/) and the client is backed by [ReactJS](http://facebook.github.io/react/) and [Flux](https://github.com/facebook/flux).




# Next Steps