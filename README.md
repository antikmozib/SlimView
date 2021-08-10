# SlimView

A fast and simple directory-based image viewer and browser for Windows, macOS and Linux.

![Screenshot](https://github.com/antikmozib/SlimView/blob/master/screenshot.jpg?raw=true)

<h1>Building</h1>

_**Requires Java 11 and Maven.**_ Java and Maven must be available on the environment path or current path.

`clone` the repository, `cd` into the root directory and execute `mvn clean`.

<h3>One-click building</h3>

|                               | Windows                               | Linux/macOS                                  |
|            -------            |:-----------                           |:------                                       |
| Build and run with system JRE | `build.bat` and `run.bat`             | `build.sh` and `run.sh`                      |
| Build and run with custom JRE | `jlink-build.bat` and `jlink-run.bat` | `jlink-build.sh` and `jlink-run.sh`          |
