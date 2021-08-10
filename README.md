# SlimView

A fast and simple directory-based image viewer and browser for Windows, macOS and Linux.

![Screenshot](https://github.com/antikmozib/SlimView/blob/master/screenshot.jpg?raw=true)

<h1>Building</h1>

**Requires Java 11 and Maven**

`clone` the repository, `cd` into the root directory and execute `mvn clean`.

Two sets of four batch files are provided to ease building and running:

* `build.bat` and `run.bat`: Builds the program using Maven and runs it with the system JDK.
* `jlink-build.bat` and `jlink-run.bat`: Builds the program using Maven, produces a native jlink image and uses that to run the program.
