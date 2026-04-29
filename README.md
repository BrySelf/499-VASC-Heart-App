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

***LIGHTING AND SUBJECT STILLNESS***
Accurate photoplethysmography (PPG) detection is highly dependent on consistent lighting and a still subject. The camera-based heart rate
algorithm works by detecting subtle color changes in facial skin caused by blood flow, and creating a usable signal from changes that are
only perceptible at the pixel level. Poor or fluctuating lighting (e.g. flickering fluorescents, backlighting, or shadows) introduces noise
that can mask or mimic these signals that we are trying to see.
Similarly, subject movement causes motion artifacts that are often orders of magnitude larger than the PPG signal itself. Even minor head
shifts or facial movements can corrupt the detection window. For best results, ensure the subject is in a well-lit, evenly illuminated
environment, Natural or diffuse indoor light works best. Ensure they remain as still as possible during measurement.
Band pass filtering helps, but cannot fully compensate for poor capture conditions.

***NOT A MEDICAL DEVICE***
This device is for research and educational purposes only. This product is not intended to treat, cure, or diagnose, any disease or ailment. 

***THIS APP IN ITS CURRENT FORM IS ONLY A DEMO***
This application is moreso just a showcase of the UI created, with backend logic being mostly packaged seperately in a Python program.
To run the program that actually will estimate heart rate, go to the seperately packaged python program.

