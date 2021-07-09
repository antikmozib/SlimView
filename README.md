# SlimView

A super-fast and simple directory-based image viewer and browser for Windows, macOS and Linux.

![Screenshot](https://github.com/antikmozib/SlimView/blob/master/screenshot.png?raw=true)

<h1>Building</h1>

_Requires [Maven](https://maven.apache.org/)_

`clone` the repository, `cd` into the root directory and execute `mvn clean`.

Three Windows batch files are provided to ease building and running:

* `build.bat` and `run.bat`: Builds and runs the program using Maven.
* `jlink-run.bat`: Builds the program using Maven, produces a native jlink image and uses that to run the program.
