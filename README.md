# [Touch Portal](https://www.touch-portal.com/) Plugin SDK

This Project is a base SDK to create a Touch Portal Plugin using Java and Gradle

## Releases

Last version is 2.0.0

Go to [releases](https://github.com/ChristopheCVB/TouchPortalPluginSDK/releases)

## Get Started

- Clone/Download this project
- Create a new Gradle Java Module
- Copy the `build.gradle` from the Sample module
- Create a class extending `TouchPortalPlugin` containing
```
    public static void main(String[] args) {
        if (args.length >= 1) {
            if (COMMAND_START.equals(args[0])) {
                System.out.println("START Command");
                
                MyPlugin myPlugin = new MyPlugin();
                myPlugin.connectThenPairAndListen(myPlugin);
            }
        }
    }
```
- Implement the interface methods
- Use the Annotations or edit the `src/main/resources/entry.tp` to add your actions, states and events
- Rename the properties `mainClassPackage` and `mainClassSimpleName` in your `build.gradle`
- Add the Plugin icon with the following path and name `src/main/resources/images/icon-24.png`
- Start calling the actions in the `onReceive(JSONObject message)` method

## Build

Use the common `gradlew clean` task to clean your build folder

Use the `gradlew packagePlugin` task to pack your plugin into a `.tpp` file