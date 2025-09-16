# Kramelix

This GitHub repositiory is for CSI 4900 - Honours Project.

## Description

Kramelix is a virtual AI assistant that takes emotion into consideration and performs basic level Android phone tasks.

## Installation / Setup

1. Download and install [Android Studio Narwhal 3](https://developer.android.com/studio)

<!-- Add instruction on what dependencies, etc. are needed to run app -->

## Gradle
- Open the Kramelix_app folder as your top project folder
-   Sync build files (should auto-install Gradle if needed) by clicking the build (hammer icon) button, and the app configuration should appear automatically
-   Run the emulator first
-   Start the app (play button at the top)
-   Everything should work?

## Usage

<!-- Add instruction on how to run the project after installation -->

### Microphone Setup on Emulator

To record audio through Android Studio emulator devices, ensure that the following is setup:

**Step 1:**

-   In the emulator menu settings (located above the emulator screen), click the Extended Controls button
    ![enable_mic_to_vm](/docs/imgs/enable_mic_to_vm.png)
-   Select Microphone tab, toggle ON "Virtual microphone uses host audio input"
    ![extended_controls_menu](/docs/imgs/extended_controls_menu.png)

**Step 2:**

-   In the emulator, open the Settings App and search Microphone. Select Microphone - Permission manager
-   Under Allowed only while in use section, select the app _Kramelix_
-   Select the option "Allow only while using the app"
    ![microphone_permission](/docs/imgs/microphone_permissions.png)
