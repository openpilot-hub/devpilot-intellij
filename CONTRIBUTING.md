## Contributing

Welcome to DevPilot intellij plugin repository. We are glad you are here!

Everyone is welcome to contribute to this repository.

### Requirements

Java 11 or higher version is required to build and test this repository.

### Building

1. Clone this repository to your local machine.
    `git clone https://github.com/openpilot-hub/devpilot-intellij.git`
2. Run the following command to build the project.
    `./gradlew runIde`
3. If you are using windows, run the following command to build the project.
    `gradlew.bat runIde`

### Update Webview

1. Clone this repository
    `https://github.com/openpilot-hub/devpilot-h5`
2. Run `pnpm install` and `pnpm run build`
3. `cp dist/ext.html ../devpilot-intellij/src/main/resources/webview/index.html`

### Testing & Checks

Before you commit your changes, please run the following command to check if there are any errors.

`./gradlew check`
