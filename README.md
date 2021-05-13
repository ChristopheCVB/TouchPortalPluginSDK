# [Touch Portal](https://www.touch-portal.com/) Plugin SDK

[![Touch Portal Plugin SDK](https://raw.githubusercontent.com/ChristopheCVB/TouchPortalPluginSDK/master/resources/TP%20Plugin%20SDK%20Logo.png)](#touch-portal-plugin-sdk)

[![Java CI with Gradle](https://github.com/ChristopheCVB/TouchPortalPluginSDK/workflows/Java%20CI%20with%20Gradle/badge.svg)](#touch-portal-plugin-sdk)
[![Library Code Coverage](https://codecov.io/gh/ChristopheCVB/TouchPortalPluginSDK/branch/master/graph/badge.svg)](https://codecov.io/gh/ChristopheCVB/TouchPortalPluginSDK)
[![Language gradle: Java](https://img.shields.io/lgtm/grade/java/g/ChristopheCVB/TouchPortalPluginSDK.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/ChristopheCVB/TouchPortalPluginSDK/context:java)

This Project is an SDK to create a Touch Portal Plugin using Java or Kotlin and Gradle.
This SDK is a COMPLETE solution which will not only help you connect to communicate with TouchPortal through Actions, Events and States, but it will also help you with the hazzle of packaging  

For further reference, the Touch Portal Plugin documentation can be found [here](https://www.touch-portal.com/api)

## Documentation

Once you have cloned this project, you can run the `gradlew javaDoc` and browse the document in the `build/docs/javadoc` of each module

## Releases

Latest version is 6.0.0

Go to [releases](https://github.com/ChristopheCVB/TouchPortalPluginSDK/releases)

## Get Started

- Clone/Download/Fork this project
- Create a new Gradle Java Module
    - Create a root level folder (i.e. `MyTPPlugin`)
    - Add an 'include' line pointing to this new folder/module on the settings.gradle file (consider commenting out the Sample and SampleKotlin for faster build times)
- Copy the `build.gradle` from the `SampleJava` or `SampleKotlin` module to your new module
    - Edit the properties `versionMajor`, `versionMinor`, `versionPatch`, `mainClassPackage` and `mainClassSimpleName`
    - Remove unnecessary dependencies
- Create a class, in the package you chose, extending `TouchPortalPlugin` and implementing `TouchPortalPlugin.TouchPortalPluginListener` (i.e. `MyTouchPortalPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener`) like the example below:

```java
public class MyTouchPortalPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
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
                // Initiate the connection with the Touch Portal Plugin System (will trigger an onInfo message with a confirmation from TouchPortal and the initial settings)
                boolean connectedPairedAndListening = myTouchPortalPlugin.connectThenPairAndListen(myTouchPortalPlugin);
            }
        }
    }

    /**
     * Called when the Socket connection is lost or the plugin has received the close Message
     */
    public void onDisconnected(Exception exception) {  }

    /**
     * Called when receiving a message from the Touch Portal Plugin System
     */
    public void onReceived(JsonObject jsonMessage) { }

    /**
     * Called when the Info Message is received when Touch Portal confirms our initial connection is successful
     */
    public void onInfo(TPInfoMessage tpInfoMessage) { }

    /**
     * Called when a List Change Message is received
     */
    public void onListChanged(TPListChangeMessage tpListChangeMessage) { }

    /**
     * Called when a Broadcast Message is received
     */
    public void onBroadcast(TPBroadcastMessage tpBroadcastMessage) { }

    /**
     * Called when a Settings Message is received
     */
    public void onSettings(TPSettingsMessage tpSettingsMessage) { }
}
```

## Development and Interaction

- The SDK will automatically callback your action methods if they only contain `@Data` annotated parameters

```java
public class MyTouchPortalPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    // ...

    /**
     * Action example with a Data Text parameter
     *
     * @param text String
     */
    @Action(description = "Long Description of Dummy Action with Data Text", format = "Set text to {$text$}", categoryId = "BaseCategory")
    private void actionWithText(@Data String text) {
        TouchPortalSamplePlugin.LOGGER.log(Level.INFO, "Action actionWithText received: " + text);
    }
    
    // ...
}
```

- Otherwise, call your actions manually in the `onReceived(JsonObject jsonMessage)` method

```java
public class MyTouchPortalPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    // ...
  
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
    }
    
    //...
}
```

- Don't forget to initialize all your services once you receive the OnInfo event. The TPInfoMessage will also contain the initial values of your settings.

```java
public class MyTouchPortalPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    // ...
  
    public void onInfo(TPInfoMessage tpInfoMessage) {
        // TPInfoMessage will contain the initial settings stored by TP
        // -> Note that your annotated Settings fields will be up to date
      
        // continue plugin initialization
    }
    
    // ...
}
```

- Finally, send messages back to TouchPortal when you want to update your states

```java
public class MyTouchPortalPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    // ...
  
    @State(defaultValue = "Default Value", categoryId = "SecondCategory")
    private String customStateText;
  
    @Action(description = "Long Description of Dummy Action with Data Text", format = "Set text to {$text$}", categoryId = "BaseCategory")
    private void actionWithText(@Data String text) {
      // ... do something then update state
      this.customStateText = "new state value";
      this.sendStateUpdate(MyTouchPortalPluginConstants.BaseCategory.States.CustomStateText.ID, this.customStateText, true);
    }
    
    // ...
}
```

## Use Annotations to describe your plugin and package it

- The provided Annotations help you in the automatic generation of the `entry.tp` file (necessary for packaging and deployment of your plugin)
- Current supported annotations include: Plugin, Category, Action, Data, State, Event and Setting
- More examples can be found in the Sample module...

```java
// imports ...

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

    /**
     * State and Event definition example
     */
    @State(defaultValue = "1", categoryId = "BaseCategory")
    @Event(valueChoices = {"1", "2"}, format = "When customStateWithEvent becomes $val")
    private String customStateWithEvent;

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

## Prepackaging

- Add the Plugin icon and extra resources into the `src/main/resources/` directory of your module

## Build

- Use the common `gradlew clean` task to clean your build directories.
- Use the common `gradlew build` task to build your project with the Annotations. 
- Use the `gradlew packagePlugin` task to pack your plugin into a `.tpp` file. Output files will be in your module `build/plugin` directory.

## Debugging tips

- A clean Touch Portal installation won't accept plugin connections by default. You need to install your plugin first on TouchPortal to 'jumpstart' the Plugin listening service and then restart Touch Portal.
- Using IntelliJ, you can also create a Configuration to start and debug the plugin right from the IDE
  - Run > Edit Configurations
  - Add New Configuration (`+`)
  - Select Application
    - Name: `TPP Start`
    - Module: `Java 8 (1.8)`
    - ClassPath Module (`-cp`): `YourModule.main`
    - Main Class: `your.package.YourTouchPortalPlugin`
    - Arguments: `start`
    - Working Directory: `YourModule/build/plugin/YourTouchPortalPlugin`
[![Touch Portal Plugin SDK Gradle Application Configuration](https://raw.githubusercontent.com/ChristopheCVB/TouchPortalPluginSDK/master/resources/TP%20Plugin%20SDK%20Gradle%20Application%20Configuration.png)](#debugging-tips)

## ROADMAP

The roadmap can be found [here](https://github.com/ChristopheCVB/TouchPortalPluginSDK/projects/1)
