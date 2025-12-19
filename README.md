# 🐈‍⬛ Kramelix

This GitHub repository is for CSI 4900 - Honours Project.

## 📝 Description

Kramelix is a virtual AI assistant that takes emotion into consideration and performs basic-level Android phone tasks.
A user has the choice to have the LLM to detect their emotion through text or tone. Changing between modes using a toggle button.

## 🔨 Installation / Setup

1. Download and install [Android Studio Narwhal 3](https://developer.android.com/studio).
2. Clone this repository.
3. In Android Studio, open the `Kramelix_app` folder as the top-level project folder
4. Follow the instructions in [Add API key](#add-api-key)
5. Follow the instructions in [Add IP Address](#add-ip-address)
6. Follow the instructions in [Microphone Setup on Emulator](#microphone-setup-on-emulator)
7. In the bottom-left corner, click the build button (hammer icon) to sync all build files; if the build button doesn't appear, navigate from the top left main menu instead, and click "Assemble Project" from the build options dropdown. It may take a while to run, but Gradle should automatically install during this step, and a confirmation message ("BUILD SUCCESSFUL") should appear at the end.
8. At the top, the app configuration should be set up automatically.
9. Run the emulator.
10. Click the "run app" button, and the Kramelix app should open within the emulator's display.

### Add API key

To run the app using the LLM, you will need to add an API key to your local.properties file:

1. Navigate to your local.properties file in the app structure
2. Enter: `OPENAI_API_KEY=[YOUR API KEY]`
3. Save file and in the Files menu on Android Studio, click Sync Project with Gradle files

### Add IP Address

To run the tone detection for the LLM, you will need to add your IP address to your local.properties file:

1. In a command prompt/terminal, run `ipconfig`
2. Copy the IPv4 address (or IPv6 address if applicable) of your LAN
3. Follow the instrutions in: [SETUP.md](Kramelix_app/emotion_training/SETUP.md#file-setup)

### Microphone Setup on Emulator

To record audio through Android Studio emulator devices, ensure that the following is set up:

**Step 1:**

-   In the emulator menu settings (located above the emulator screen), click the Extended Controls button
    ![enable_mic_to_vm](/docs/imgs/enable_mic_to_vm.png)
-   Select Microphone tab, toggle ON "Virtual microphone uses host audio input"
    ![extended_controls_menu](/docs/imgs/extended_controls_menu.png)

**Step 2:**

> [!NOTE]
> This step may not be required on some devices.

-   In the emulator, open the Settings App and search Microphone. Select Microphone - Permission Manager
-   Under the "Allowed only while in use" section, select the app _Kramelix_
-   Select the option "Allow only while using the app"
  
    ![microphone_permission](/docs/imgs/microphone_permissions.png)

## 🚀 Usage

Once the above instructions have been completed, the app can be used. Click the microphone/record button to talk to the LLM. When you are done speaking, click the button again and wait for the LLM's response. The response will be displayed on the screen and read back to you.

**Swap Between How Emotion is Detected**
- Use the pink toggle button in the top right of the app to switch between the LLM using text to detect emotion and the LLM using tone to detect emotion.

**Basic Communication**
- Use the LLM to talk about your day, vent concerns, or get advice. Talk to it however you want!

**Tasks to Ask the LLM**
- Play Music 🎵: ask the LLM to play a song given the song title, the artist name, or to randmly play a song based on a music genre
    - When music is playing, it can be paused/resumed or stopped on the app or through the notification center
- Set a Timer ⏰: ask the LLM to set a timer to go off in _n mins/hours_
- Call a Contact ☎️: ask the LLM to call a specific person from your contacts (_Note: emulators cannot be used for this_)

## ❗ Troubleshooting Builds / GitIgnore Information

-   To completely reset the project build, delete the local instances of `/Kramelix_app/.gradle`, `/Kramelix_app/build`, `/Kramelix_app/app/build`, and `/Kramelix_app/app/.cxx`. Afterwards, start from step 4 of the Installation/Setup section above, and these folders should rebuild.
-   When committing files, the `.gitignore` file will automatically filter out any local build folders that don't need to be shared between developers. This mainly includes the above-mentioned folders^. **Anything else should be committed.**

## ✨ Authors
💛 **Amy Huang**

💚 **Alex Oprea**

🩷 **Kristen Duong**
