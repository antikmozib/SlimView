# SlimView

A fast and simple directory-based image viewer and browser for Windows, macOS and Linux.

![Screenshot](https://github.com/antikmozib/SlimView/blob/master/screenshot.jpg?raw=true)

## Building

### Requirements

* Java 11
* Maven 3.6.3+
* Cygwin (if building the custom JRE on Windows)

### Building with Maven

`clone` the repository, `cd` into the root directory and execute `mvn clean package`.

### Building custom JRE

Execute the script `build.sh` to produce a native custom JRE for running the app. The script supports the following flags:
* `e`: Make a platform-depedendent executable
* `r`: Make a platform-depedendent installer
* `l`: Launch when build completes
* `s`: Specify app parameters when launching

For example, executing `./build.sh -erl -s '--uninst'` will build the app, make an executable, produce an installer and finally launch the app with the parameter `--uninst`.
