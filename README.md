# [Touch Portal](https://www.touch-portal.com/) Plugin SDK

This Project is an SDK to create a Touch Portal Plugin using Java and Gradle

## Releases

Last version is 2.2.0

Go to [releases](https://github.com/ChristopheCVB/TouchPortalPluginSDK/releases)

## Get Started

- Clone/Download this project
- Create a new Gradle Java Module (i.e. `MyTPPlugin`)
- Copy the `build.gradle` from the `Sample` module
- Create a class extending `TouchPortalPlugin` (i.e. `MyTouchPortalPlugin extends TouchPortalPlugin`) containing:
```
    public static void main(String[] args) {
        if (args != null && args.length == 2) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                MyTouchPortalPlugin myTouchPortalPlugin = new MyTouchPortalPlugin(args);
                try {
                    // Load a properties File
                    myTouchPortalPlugin.loadProperties("plugin.config");
                    // Get a property
                    System.out.println(myTouchPortalPlugin.getProperty("samplekey"));
                    // Set a property
                    myTouchPortalPlugin.setProperty("samplekey", "Value set from Plugin");
                    // Store the properties
                    myTouchPortalPlugin.storeProperties();
                }
                catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                // Initiate the connection with the Touch Portal Plugin System
                boolean connectedPairedAndListening = myTouchPortalPlugin.connectThenPairAndListen(touchPortalPluginExample);

                if (connectedPairedAndListening) {
                    // Update a State with the ID from the Generated Constants Class
                    boolean stateUpdated = myTouchPortalPlugin.sendStateUpdate(TouchPortalPluginExampleConstants.BaseCategory.States.CustomState.ID, "2");
                }
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

Use the common `gradlew clean` task to clean your build directories.

Use the common `gradlew build` task to build your project with the Annotations.

Use the `gradlew packagePlugin` task to pack your plugin into a `.tpp` file. Output files will be in your module `build/plugin` directory.

## ROADMAP

The roadmap can be found [here](https://github.com/ChristopheCVB/TouchPortalPluginSDK/projects/1)
