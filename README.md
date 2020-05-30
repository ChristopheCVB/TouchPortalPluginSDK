# [Touch Portal](https://www.touch-portal.com/) Plugin SDK

This Project is an SDK to create a Touch Portal Plugin using Java and Gradle

## Releases

Last version is 2.1.0

Go to [releases](https://github.com/ChristopheCVB/TouchPortalPluginSDK/releases)

## Get Started

- Clone/Download this project
- Create a new Gradle Java Module (i.e. `MyTPPlugin`)
- Copy the `build.gradle` from the `Sample` module
- Create a class extending `TouchPortalPlugin` containing (i.e. `MyTouchPortalPlugin extends TouchPortalPlugin`)
```
    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                MyTouchPortalPlugin myTouchPortalPlugin = new MyTouchPortalPlugin();
                myTouchPortalPlugin.connectThenPairAndListen(myTouchPortalPlugin);
            }
        }
    }
```
- Edit the properties `mainClassPackage` and `mainClassSimpleName` in your `build.gradle`
- Implement the interface methods
- Use the Annotations or edit the `src/main/resources/entry.tp` to add your actions, states and events
- Add the Plugin icon with the following path and name `src/main/resources/images/icon-24.png`
- Start calling the actions in the `onReceive(JSONObject message)` method

## Build

Use the common `gradlew clean` task to clean your build folder

Use the `gradlew packagePlugin` task to pack your plugin into a `.tpp` file. Output files will be in your module `build/plugin` directory

## TODO

- [ ] Create a gradle plugin for the `packagePlugin` task
- [ ] Automatically call actions
