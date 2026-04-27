# 499-VASC-Heart-App
***This app uses a React Native + MediaPipe framework to run***

VASC Heart Rate App employs camera based heart rate detection with ~± 5 accuracy.
Face detection is done through MediaPipe and OpenCV, with FFT functions being done through the JTransforms library.
Frontend is done through TypeScript in the React Native framework, with backend being written in Java/Kotlin for Android use.

***SETUP REQUIREMENTS***
Node.js (>=version 18)
Android Studio
Android device/emulator
Java (Version 17 is Recommended)

***GRADLE INSTRUCTIONS***
This project uses Gradle 8.x, ensure the Gradle distribution URL is set properly in /android/gradle/wrapper/gradle-wrapper.properties
Current developer setup for this is : distributionUrl=https://services.gradle.org/distributions/gradle-8.7-bin.zip
If this does not work ensure you are using a compatable JDK version such as 11 or 17.
If sync fails attempt a gradle clean by running "./gradlew clean" in the android folder

***EXECUTION INSTRUCTIONS***
Ensure you have an android emulator running, or have your device connected to android studio.
Split your terminal and from the project root, run "npx react-native start" in one terminal and "npx react-native run-android" in the other.
The app should launch on your emulator/device.
