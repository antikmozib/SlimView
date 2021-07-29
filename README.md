# SlimView

A fast and simple directory-based image viewer and browser for Windows, macOS and Linux.

![Screenshot](https://github.com/antikmozib/SlimView/blob/master/screenshot.jpg?raw=true)

<h1>Building</h1>

**Tested with JDK 11 on Windows 10 and Linux Mint 20.2**

_Java, Maven and [JavaFX jmods](https://openjfx.io/openjfx-docs/#modular) (required for producing jlink images) must be set as environment variables. Alternatively, use an IDE like IntelliJ IDEA to import as a Maven project._

`clone` the repository, `cd` into the root directory and execute `mvn clean`.

Four Windows batch files are provided to ease building and running:

* `build.bat` and `run.bat`: Builds the program using Maven and runs it with the system JDK.
* `jlink-build.bat` and `jlink-run.bat`: Builds the program using Maven, produces a native jlink image and uses that to run the program.
