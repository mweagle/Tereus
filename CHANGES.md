## CHANGES

### v0.0.2
  - The `StackName` value is now defined either in the stack definition as `CloudFormationTemplate("STACK_NAME")` or referenced in the Patch definition file as `CloudFormationUpdate("_")`.  Consider [process.pid](https://nodejs.org/api/process.html#process_process_pid), the `USER` in  [process.env](https://nodejs.org/api/process.html#process_process_env), `new Date().getTime()`, or [node-uuid](https://github.com/broofa/node-uuid) if you need `unique` stacknames.
  - Added support for automatically building Gradle AWS Lambda projects.
    - `gradle build` is triggered if _build.gradle_ file is found in Lambda source directory
    - `gradle build` **MUST** produce a _*.jar_ file in the Lambda source directory
  - Added `Patch.Lambda` to streamline AWS Lambda updates
  - Implemented stable ZIP for content-addressable AWS Lambda S3 keynames
  - Migrated to IntelliJ (it's worth the [subscription](https://www.jetbrains.com/toolbox/))
  - Additional diagnostics for gradle build.

### v0.0.1
  - Initial release