#!/usr/bin/env bash -e
TEREUS_JAR_ARG=${TEREUS_JAR:-"../../build/libs/Tereus.jar"}
S3_BUCKET_ARG=${S3_BUCKET:-"weagle"}
AWS_REGION_ARG=${AWS_DEFAULT_REGION:-"us-west-2"}
java -jar $TEREUS_JAR_ARG create --template stack.js --bucket $S3_BUCKET_ARG  --region $AWS_REGION_ARG --dry-run
java -jar $TEREUS_JAR_ARG update --patch patch.js --region $AWS_REGION_ARG  --dry-run