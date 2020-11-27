## Office Entry
This is an Android application with 2 main features: mask detection and temperature checking. This application is targeted to the temi robot, although it is possible to run the application in any Android devices with API level 23 or above.

### Temperature Checking
This feature requires TM-IR thermal imaging camera (version 3). The camera will report the highest detected temperature from its point of view. To receive the data, the application and the camera needs to be connected to the same WiFi connection. Since the camera does not have any interface, a socket connection is required to send the WiFi credentials to the camera.

<br>
<img src="https://drive.google.com/uc?export=view&id=19NydPBLsRjvOlIpx4_4ImVfJndwrKvOs" 
  alt="Temperature Checking Demo"
  width=1000
  height=600>
  
### Mask Detection
By using the front camera, this application runs object detection to the captured frames. The model used behind is customized to search for a person's face wearing a mask and not wearing a mask.

<br>
<img src="https://drive.google.com/uc?export=view&id=1NuYBFh7GGkfbJWCRKReIyDvzT56Lv6SM" 
  alt="Mask Detection Demo"
  width=333
  height=500 >
