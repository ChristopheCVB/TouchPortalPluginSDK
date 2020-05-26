# [Touch Portal](https://www.touch-portal.com/) Plugin SDK

This Project is a base SDK to create a Touch Portal Plugin using Java and Gradle

## Releases

Last version is 1.0.0

Go to [releases](https://github.com/ChristopheCVB/TouchPortalPluginSDK/releases)

## Get Started

- Clone/Download this project
- Rename the property in `settings.gradle`
- Edit the `src/main/resources/entry.tp` to add your actions
- Replace the Plugin icon in `src/main/resources/images/icon-24.png`
- Create a class that extends `TouchPortalPlugin` containing
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
- Start writing the actions code in the `onReceive(JSONObject message)` method