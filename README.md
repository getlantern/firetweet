# FireTweet

FireTweet is an Android app powered by Lantern that gives direct unblocked
access to Twitter from anywhere in the world.

<img src="screenshots/screenshot1.jpg" height="330px" width="200px">
<img src="screenshots/screenshot2.jpg" width="200px">
<img src="screenshots/screenshot3.jpg" width="200px">

You can download the latest build of FireTweet
[here](https://github.com/firetweet/downloads/blob/master/firetweet.apk?raw=true).

## Building Firetweet

### Building from Android Studio

#### Prerequisites

* [Android Studio][2]
* git

Download the most recent copy of the Firetweet's source code using `git`:

```
mkdir -p ~/AndroidstudioProjects
cd ~/AndroidstudioProjects
git clone https://github.com/getlantern/firetweet.git
```

In the welcome screen choose the "Open an existing Android Studio" option and
select the `firetweet` folder you just checked out with git.

![screen shot 2015-06-03 at 3 05 47 pm](https://cloud.githubusercontent.com/assets/385670/7970218/19ad1676-0a02-11e5-9480-b51c4cd1bdde.png)

Wait until Android Studio finishes importing the project.

![screen shot 2015-06-03 at 4 26 43 pm](https://cloud.githubusercontent.com/assets/385670/7971837/5dcf7172-0a0d-11e5-95be-8352444fea75.png)

After a few minutes you'll end up with a blank workspace, click the `1.
Project` tab from the left side of the screen and make sure the combo box near
the play button says `firetweet`.

![screen shot 2015-06-03 at 4 30 07 pm](https://cloud.githubusercontent.com/assets/385670/7971918/0e21190e-0a0e-11e5-8eb1-16f5aecc5bc4.png)

If you want to test Firetweet on an Android emulator or into a real device,
just click the play button and choose on which device you want to deploy
Lantern. If you want to test this on an emulator make sure you're using the ARM
architecture.

To build FireTweet select the `Make Project` action from the `Build` menu.

![screen shot 2015-06-03 at 4 34 12 pm](https://cloud.githubusercontent.com/assets/385670/7971971/64eedf50-0a0e-11e5-8914-da487955d016.png)

This will create a `./firetweet/build/outputs/apk/firetweet-fdroid-debug.apk`
file that you can install on an android device with the help of `adb`:

```
adb install ./firetweet/build/outputs/apk/firetweet-fdroid-debug.apk
```

### Building from the Command Line (beta, for development only)

#### Prerequisites

* Java Development Kit 1.7
* Git

#### Building, installing and running

Build the Debug target:

```
make build-debug
```

Install it:

```
make install
```

Run the app on the device from the command line:

```
make run
```

By default, all three tasks will be run in order with:

```
make
```


## How is Lantern included in Firetweet?

We created a really small version of Lantern that can be compiled for Android
phones and used by Android apps. This library is bundled with Firetweet, you
can always find the latest version of the library
[here](https://github.com/getlantern/firetweet/tree/master/firetweet/src/main/jniLibs/armeabi-v7a).

If you prefer to build this binary blob for yourself you may check out the
[Lantern building instructions](https://github.com/getlantern/lantern), in
particular the "Creating libgojni.so" section.


## VirtualBox Android development images

### Why use a Virtualbox image instead of the Android Emulator or a real device?

There are several reasons for doing so. When comparing with the Android Emulator, the main reason is that Virtualbox is faster. It virtualizes the x86 architecture, which introduces less overhead than full ARM emulation. When comparing with a real device, there are also reasons for using Virtualbox instead:

* On-screen development. This might be useful for remote pair programming or visual testing.
* Screen size customization.
* Easier build and bug reproducibility.

One thing to note is that the majority of Android devices use the ARM architecture. Virtualbox relies on the x86 architecture instead. However, the emulation feature called _Houdini_ binary translation allows for instruction translation, which means that we can run _native_ ARM code on our x86 emulator.

The Android x86 images are based on this project: http://www.android-x86.org/

You can find the latest image [here](https://s3.amazonaws.com/lantern-android-development-images/Android+4.4.ova)

### Installing on the VirtualBox image

1. Make sure that port forwarding is set from 5555 (host) to 5555 (guest) for the main network interface.
2. Run the Virtualbox image.
3. Connect with ADB
```
adb connect localhost:5555
```
4. Check that the device is properly connected
```
adb devices
```
5. Run make (it will compile, upload and run the app on VirtualBox)
```
make
```

### Development notes

#### Screen orientation

* Press F12 two times in less than 2 seconds = Rotate 90º to the LEFT.
* Press F11 two times in less than 2 seconds = Rotate 90º to the RiGHT.
* Press F10 two times in less than 2 seconds = Rotate 180º.
* Press F9 two times in less than 2 seconds = Normal view 0/360º.

#### Screen size

* Using the kernel VGA configuration option:

When in GRUB, hit 'e' to edit the first entry, then 'e' again to edit the first line. Append the text 'vga=ask' at the end of the boot line. Choose any of the display sizes, but the depth must be 16.

* Using an app (recommended):

Use the app called _Resolution Changer_ and activate the resolution overriding. There is no need to reboot after this.

#### Misc

* For better UI integration, disable mouse integration (Host+I)
* Certain apps, including Firetweet reorient the screen. This will remap the mouse coordinates and motion, which will drive you crazy! First try rotating setting the screen orientation to landscape. Then, you can try also setting a screen size with _Resolution Changer_ to a new height/width that inverts these values.



## Open Source

FireTweet was forked from [Twidere][1], an Open Source client for Twitter.

> Twidere - Twitter client for Android
>
> Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
>
> This program is free software: you can redistribute it and/or modify
> it under the terms of the GNU General Public License as published by
> the Free Software Foundation, either version 3 of the License, or
> (at your option) any later version.
>
> This program is distributed in the hope that it will be useful,
> but WITHOUT ANY WARRANTY; without even the implied warranty of
> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
> GNU General Public License for more details.
>
> You should have received a copy of the GNU General Public License
> along with this program.  If not, see <http://www.gnu.org/licenses/>.

[1]: https://github.com/TwidereProject/Twidere-Android/
[2]: http://developer.android.com/tools/studio/index.html
