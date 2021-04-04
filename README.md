# [Touch Portal](https://www.touch-portal.com/) Plugin SDK

[![Touch Portal Plugin SDK](https://raw.githubusercontent.com/ChristopheCVB/TouchPortalPluginSDK/master/resources/TP%20Plugin%20SDK%20Logo.png)](#touch-portal-plugin-sdk)

[![Java CI with Gradle](https://github.com/ChristopheCVB/TouchPortalPluginSDK/workflows/Java%20CI%20with%20Gradle/badge.svg)](#touch-portal-plugin-sdk)
[![Library Code Coverage](https://codecov.io/gh/ChristopheCVB/TouchPortalPluginSDK/branch/master/graph/badge.svg)](https://codecov.io/gh/ChristopheCVB/TouchPortalPluginSDK)
[![Language gradle: Java](https://img.shields.io/lgtm/grade/java/g/ChristopheCVB/TouchPortalPluginSDK.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/ChristopheCVB/TouchPortalPluginSDK/context:java)

This Project is an SDK to create a Touch Portal Plugin using Java or Kotlin and Gradle

The Touch Portal Plugin documentation can be found [here](https://www.touch-portal.com/api)

## Documentation

Once you have cloned this project, you can run the `gradlew javaDoc` and browse the document in the `build/docs/javadoc` of each module

## Releases

Latest version is 6.0.0

Go to [releases](https://github.com/ChristopheCVB/TouchPortalPluginSDK/releases)

## Get Started

- Clone/Download/Fork this project
- Create a new Gradle Java Module (i.e. `MyTPPlugin`)
- Copy the `build.gradle` from the `Sample` module to your new module and remove unnecessary dependencies
- Create a class, in the package you chose, extending `TouchPortalPlugin` (i.e. `MyTouchPortalPlugin extends TouchPortalPlugin`) like the example below:
```java
public class MyTouchPortalPlugin extends TouchPortalPlugin {
    /**
     * Logger
     */
    private final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());
    
    /**
     * Constructor calling super
     */
    public MyTouchPortalPlugin() {
        super(true);
    }

    public static void main(String[] args) {
        if (args != null && args.length == 1) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize your Plugin
                MyTouchPortalPlugin myTouchPortalPlugin = new MyTouchPortalPlugin();
                // Initiate the connection with the Touch Portal Plugin System
                boolean connectedPairedAndListening = myTouchPortalPlugin.connectThenPairAndListen(myTouchPortalPlugin);
            }
        }
    }

    @Override
    public void onDisconnected(Exception exception) {
        // Socket connection is lost or plugin has received close message
        if (exception != null) {
            exception.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public void onReceived(JsonObject jsonMessage) {
        // Check if ReceiveMessage is an Action
        if (ReceivedMessageHelper.isTypeAction(jsonMessage)) {
            // Get the Action ID
            String receivedActionId = ReceivedMessageHelper.getActionId(jsonMessage);
            if (receivedActionId != null) {
                // Manually call the action methods which not all parameters are annotated with @Data
                switch (receivedActionId) {
                    // case ...:
                    // break;
                }
            }
        }
        // dummyWithData and dummySwitchAction are automatically called by the SDK
    }

    //...
}
```
- Edit the properties `mainClassPackage` and `mainClassSimpleName` in your `build.gradle`
- Implement the interface methods
- Generating the `entry.tp` file using Annotations (More examples can be found in the Sample module)
```java
package com.github.ChristopheCVB.TouchPortal.sample;

// import ...

@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "My Touch Portal Plugin")
public class MyTouchPortalPlugin extends TouchPortalPlugin {
    //...

    /**
     * Action example that contains a dynamic data text
     *
     * @param text String
     */
    @Action(description = "Long Description of Dummy Action with Data", format = "Set text to {$text$}", categoryId = "BaseCategory")
    private void dummyWithData(@Data String text) {
        LOGGER.log(Level.Info, "Action dummyWithData received: " + text);
    }

    private enum Categories {
        /**
         * Category definition example
         */
        @Category(name = "My Touch Portal Plugin", imagePath = "images/icon-24.png")
        BaseCategory
    }

    //...
}
```
- Add the Plugin icon into the following directory `src/main/resources/` of your module
- If your declared Methods only contain `@Data` annotated parameter, it will be called automatically by the SDK, otherwise call your actions in the `onReceive(JsonObject jsonMessage)` method

## Build

Use the common `gradlew clean` task to clean your build directories.

Use the common `gradlew build` task to build your project with the Annotations.

Use the `gradlew packagePlugin` task to pack your plugin into a `.tpp` file. Output files will be in your module `build/plugin` directory.

## ROADMAP

The roadmap can be found [here](https://github.com/ChristopheCVB/TouchPortalPluginSDK/projects/1)
