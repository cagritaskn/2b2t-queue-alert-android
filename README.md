<p align="center"><img src="files/logo.png" width="128" height="128"></p>

# About
This app tracks and notifies you of your 2B2T queue position on your Android phone.

# Usage

## Installation
To get started, download the following files:
- [PC-Server-2B2TQueueAlert.exe](https://github.com/cagritaskn/2b2t-queue-alert-android/releases/download/release/PC-Server-2B2TQueueAlert.exe) for your PC.
- [Android-Client-2B2TQueueAlert.apk](https://github.com/cagritaskn/2b2t-queue-alert-android/releases/download/release/Android-Client-2B2TQueueAlert.apk) for your Android phone.
Then install the APK file on your Android phone. The Play Protect might give you a warning telling you "It's recommended to scan this app." since it does not recognise the package name (It needs to be published to Play Store in order to bypass this warning.) you can skip scanning or scan and install it since it **doesn't** have harmful code.

## Initialize
Make sure that your Android phone and your PC are connected to the same network connection.
To get started, first run the server **(PC-Server-2B2TQueueAlert.exe)** on your PC and enter the IP address provided by the PC server into the Android app's **(Android-Client-2B2TQueueAlert.apk)** IP address input field. Be sure to grant notification permissions for the Android app to ensure the app functions correctly.

> [!IMPORTANT]
> Some Android operating systems has a feature called **Battery Optimization** which may hibernate the app after some time. **The 2B2T Queue Alert Android app asks and redirects you to the related settings section but if you couldn't disable it that way or if you get an error trying to do it you should read this.** Be sure to disable battery optimization for this app if exists. Here is instructions of disabling the battery optimization for any app: **[How to disable battery optimization for applications](https://github.com/cagritaskn/2b2t-queue-alert-android/blob/main/DISABLING_BATTERY_OPTIMIZATION.md)**. Some Android phones have this setting in the application page. If you couldn't do it by following this manual then try going into `Settings>Applications>2B2T Queue Alert>Battery` in this window, choose "Not restriced" (or anything with the same meaning).

> [!NOTE]
> If you have any kind of **Anti-Spam** (Chat spam blocking) feature turned on, it will probably block the chat queue announcer. So If you are using any kind of anti spam feature, **turn it off** in order to make the app work.

# How does it work ?
The PC application reads data from Minecraft's latest.log file, uploads this data to a local IP address, and allows the Android phone, which is connected to the same network, to access and display this data. The Android phone then sends notifications based on the data received from the PC.

## Does it have viruses, is there any chance of my coordinates getting leaked ?
The source codes of [PC-Server-2B2TQueueAlert.exe](https://github.com/cagritaskn/2b2t-queue-alert-android/releases/download/release/PC-Server-2B2TQueueAlert.exe) executable and [Android-Client-2B2TQueueAlert.apk](https://github.com/cagritaskn/2b2t-queue-alert-android/releases/download/release/Android-Client-2B2TQueueAlert.apk) package are shared in this repository. That means you can see what these apps do if you inspect the codes. If you can understand the Kotlin and Python languages, give it a peek. You can scan for viruses if you want too. It doesn't send any data out of the local network and the processed data only includes the queue lines in the chat. so your coordinates aren't going out of your network. It's clean believe me. I made this project for myself and decided to share it (Too broke to buy the priority queue).

> [!NOTE]
> A small portion (5 in 74) of antivirus software detects the PC server as a trojan. **This is false positive** and it's being caused by the PC server application copying itself into a folder in order to make the `Run on Windows Startup` feature to work. It does not copy itself into any directory if you don't choose to make it run on Windows startup. If you chose to make it Run on Windows Startup then the program copies itself to `Program Files\2B2T Queue Alert\` directory. You can delete it from there if you want (No Uninstaller yet because it's standalone for now).

# Running and building the server on PC with Python
You can use the python app.py by cloning the repository to your PC, opening a terminal in the directory where the "app.py" is located (files/pc-server-python) and running the command:
```
python app.py
```

You can also build the app.py into an executable (.exe) file by cloning the repository to your PC, opening a terminal in the directory where the "app.py" is located (files/pc-server-python) and running the command:
```
pyinstaller --onefile --windowed --add-data "icon.ico;." --icon "icon.ico" app.py
```
# Building the Android app yourself
You can build the Android app yourself by cloning the repository to your PC and copying the project folder (files/android-client-project) into the projects folder of Android Studio IDE then building it from there.

# Contact
If you face any issues or errors you can contact me at:
Discord: "kegrisko." (The ID has a dot at the end)

# Screenshots of the PC server and the Android app

<img src="files/server-screenshot.png" width="256" >

<img src="files/client-screenshot.png" width="256" >

<img src="files/restart-notification-screenshot.png" width="256" >

<img src="files/queue-notification-screenshot.png" width="256" >

# Dupe
This project works well with the cactus dupe. (Planning on adding the popbob bed dupe later.)

# License
This code is licensed under the GNU General Public License v3. **You can only use this code in open-source clients that you release under the same license! Using it in closed-source/proprietary clients is not allowed.**
