# Python Backend for VASC Heart App
***INSTALL REQUIREMENTS***
Ensure you install requirements listed in the requirements.txt file. (Use a virtual environment for best results)
Run this command in your terminal:

*pip install -r requirements.txt*

This will install the necessary libraries for the program to work.

***RUN PROGRAM***
You should then run the program through the main file:

*python main.py*

This should launch a window that will show your camera (if permission is asked, allow)

***LIGHTING AND SUBJECT STILLNESS***
Accurate photoplethysmography (PPG) detection is highly dependent on consistent lighting and a still subject. The camera-based heart rate
algorithm works by detecting subtle color changes in facial skin caused by blood flow, and creating a usable signal from changes that are
only perceptible at the pixel level. Poor or fluctuating lighting (e.g. flickering fluorescents, backlighting, or shadows) introduces noise
that can mask or mimic these signals that we are trying to see.
Similarly, subject movement causes motion artifacts that are often orders of magnitude larger than the PPG signal itself. Even minor head
shifts or facial movements can corrupt the detection window. For best results, ensure the subject is in a well-lit, evenly illuminated
environment, Natural or diffuse indoor light works best. Ensure they remain as still as possible during measurement.
Band pass filtering helps, but cannot fully compensate for poor capture conditions.

Following this your bpm should appear in the window and in the terminal.
Happy measuring!
-The VASC team
