# Kramelix

This GitHub repository is for CSI 4900 - Honours Project.

## Description

Kramelix is a virtual AI assistant that takes emotion into consideration and performs basic-level Android phone tasks.

## Installation / Setup

1. Download and install [Android Studio Narwhal 3](https://developer.android.com/studio).
2. Clone this repository.
3. In Android Studio, open the `Kramelix_app` folder as the top-level project folder
4. In the bottom-left corner, click the build button (hammer icon) to sync all build files; if the build button doesn't appear, navigate from the top left main menu instead, and click "Assemble Project" from the build options dropdown. It may take a while to run, but Gradle should automatically install during this step, and a confirmation message ("BUILD SUCCESSFUL") should appear at the end.
5. At the top, the app configuration should be set up automatically.
6. Run the emulator.
7. Click the "run app" button, and the Kramelix app should open within the emulator's display.

### Troubleshooting Builds / GitIgnore Information

-   To completely reset the project build, delete the local instances of `/Kramelix_app/.gradle`, `/Kramelix_app/build`, `/Kramelix_app/app/build`, and `/Kramelix_app/app/.cxx`. Afterwards, start from step 4 of the Installation/Setup section above, and these folders should rebuild.
-   When committing files, the `.gitignore` file will automatically filter out any local build folders that don't need to be shared between developers. This mainly includes the above-mentioned folders^. **Anything else should be committed.**

<!-- Add instruction on what dependencies, etc. are needed to run app -->

## Gradle

## Usage

<!-- Add instruction on how to run the project after installation -->

### Microphone Setup on Emulator

To record audio through Android Studio emulator devices, ensure that the following is set up:

**Step 1:**

-   In the emulator menu settings (located above the emulator screen), click the Extended Controls button
    ![enable_mic_to_vm](/docs/imgs/enable_mic_to_vm.png)
-   Select Microphone tab, toggle ON "Virtual microphone uses host audio input"
    ![extended_controls_menu](/docs/imgs/extended_controls_menu.png)

**Step 2:**

-   In the emulator, open the Settings App and search Microphone. Select Microphone - Permission Manager
-   Under the "Allowed only while in use" section, select the app _Kramelix_
-   Select the option "Allow only while using the app"
    ![microphone_permission](/docs/imgs/microphone_permissions.png)
