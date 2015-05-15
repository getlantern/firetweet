# FireTweet tests

## Creating the emulator

1. Open Android Studio and start the AVD manager.
2. Press the "Create Virtual Device" button.
3. Select "Nexus 5" and press "Next".
4. You'll be asked to choose a system image, select an armeabi-v7a based ABI, like Lollipop 21.
5. Click "Next" when you're done selecting the system image.
6. Name your machine following this pattern: `droid_{api number}_{android version}_{resolution}_{density}`, for instance
   `droid_18_4-3-1_1080x1920_xxhdpi`.
7. Press "Finish" to create the emulator.

## Running the emulator via command line.

Get the name of the emulator you want to run:

```
emulator -list-avds
```

Run it like this:

```
emulator -avd droid_18_4-3-1_1080x1920_xxhdpi -dns-server 8.8.8.8,8.8.4.4
```

Wait until it's fully started and unlock it.

## Running tests

Make sure `adb devices` reports only one emulator running.

`cd` into `firetweet/tests` directory and run the `testrunner.py` script using
`monkeyrunner`.

```
monkeyrunner testrunner.py
```

