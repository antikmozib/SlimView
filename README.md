# SlimView

A fast and simple directory-based image viewer and browser for Windows, macOS and Linux.

![Screenshot](https://github.com/antikmozib/SlimView/blob/master/screenshot.jpg?raw=true)

## Building

### Requirements

* Java 11
* Maven 3.6.3+

Additionally, Cygwin is needed if building the custom JRE on Windows.

### Building with Maven

`clone` the repository, `cd` into the root directory and execute `mvn clean package`.

### Building custom JRE

Execute the script `jlink-build.sh` to produce a native custom JRE for running the app. Execute the script `jlink-run.sh` to launch the app using the custom JRE.
