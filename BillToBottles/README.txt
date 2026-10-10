BILL TO BOTTLES — NATIVE ANDROID APP (OFFLINE)
================================================

This is a native Android application written in Java. It does NOT use HTML,
a WebView, a website, or the internet. It needs no internet permission and
stores entries privately on the phone using Android's local app storage.

FEATURES
--------
- IMFL sizes: 750 ml (12 per case), 375 ml (24), 180 ml (48)
- Beer sizes: 650 ml (12 per case), 500 ml (24), 330 ml (24), 275 ml (24)
- Add case quantities and loose-bottle quantities separately
- Per-size totals, IMFL/Beer subtotals, and grand total
- Tap an entry chip to remove that entry
- Undo the most recent addition in the current session
- Clear the bill with a confirmation prompt
- Entries automatically remain saved on the phone between app launches

BUILD AN INSTALLABLE APK
------------------------
This project archive contains Android source code, not a ready-compiled APK.
Build the APK once, install it, and then the app runs fully offline.

OPTION A — GITHUB ACTIONS (no Android Studio required)
1. Extract this ZIP on a computer.
2. Create a repository on GitHub.
3. Upload the CONTENTS of the extracted BillToBottles folder to the repository.
   Keep the .github/workflows/build.yml file and its folder.
4. Open the repository's Actions tab and choose "Build APK". Run the workflow
   if it has not run automatically. Wait for the workflow to finish successfully.
5. Open the completed workflow run, scroll to Artifacts, and download
   "BillToBottles-apk". Extract that downloaded ZIP to find app-debug.apk.
6. Transfer app-debug.apk to your Android phone and tap it to install. Android
   may ask you to permit installing apps from that file manager/browser.
7. Open "Bill to Bottles". You can now turn off Wi-Fi and mobile data; the app
   performs its calculations and saves entries entirely on the phone.

OPTION B — ANDROID STUDIO
1. Open the extracted BillToBottles folder in Android Studio.
2. Let Gradle sync, then choose Build > Build Bundle(s) / APK(s) > Build APK(s).
3. Android Studio will show the path to app-debug.apk when the build completes.

NOTES
-----
- Internet may be needed once to download build tools or retrieve the APK from
  GitHub. Internet is NOT needed to use the installed app.
- App data is saved in Android's private app storage. Uninstalling the app or
  clearing its storage will delete saved entries.
- No network permission is declared in AndroidManifest.xml.
