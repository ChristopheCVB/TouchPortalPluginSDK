# [Touch Portal](https://www.touch-portal.com/) Plugin SDK

![Touch Portal Plugin SDK](https://raw.githubusercontent.com/ChristopheCVB/TouchPortalPluginSDK/master/resources/TP%20Plugin%20SDK%20Logo.png)

![Java CI with Gradle](https://github.com/ChristopheCVB/TouchPortalPluginSDK/workflows/Java%20CI%20with%20Gradle/badge.svg)

This Project is an SDK to create a Touch Portal Plugin using Java and Gradle

The Touch Portal Plugin documentation can be found [here](https://www.touch-portal.com/sdk)

## Documentation

Once you have cloned this project, you can run the `gradlew javaDoc` and browse the document in the `build/docs/javadoc` of each module

## Releases

Last version is 3.0.0

Go to [releases](https://github.com/ChristopheCVB/TouchPortalPluginSDK/releases)

## Get Started

- Clone/Download this project
- Create a new Gradle Java Module (i.e. `MyTPPlugin`)
- Copy the `build.gradle` from the `Sample` module to your new module and remove unnecessary dependencies
- Create a class, in the package you chose, extending `TouchPortalPlugin` (i.e. `MyTouchPortalPlugin extends TouchPortalPlugin`) containing:
```
    public static void main(String[] args) {
        if (args != null && args.length == 2) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                MyTouchPortalPlugin myTouchPortalPlugin = new MyTouchPortalPlugin(args);
                // Initiate the connection with the Touch Portal Plugin System
                boolean connectedPairedAndListening = myTouchPortalPlugin.connectThenPairAndListen(touchPortalPluginExample);
            }
        }
    }
```
- Edit the properties `mainClassPackage` and `mainClassSimpleName` in your `build.gradle`
- Implement the interface methods
- Creating the `entry.tp` file
  - Use Annotations (Example can be found in the Sample module)
  - Create and edit the file `src/main/resources/entry.tp` of your module to add your actions, states and events
- Add the Plugin icon into the following directory `src/main/resources/` of your module
- Start calling the actions in the `onReceive(JSONObject jsonMessage)` method

## Build

Use the common `gradlew clean` task to clean your build directories.

Use the common `gradlew build` task to build your project with the Annotations.

Use the `gradlew packagePlugin` task to pack your plugin into a `.tpp` file. Output files will be in your module `build/plugin` directory.

## ROADMAP

The roadmap can be found [here](https://github.com/ChristopheCVB/TouchPortalPluginSDK/projects/1)
