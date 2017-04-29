# Simple_Tracker
Tracks a user's movements using simple GPS sensing

There are four activities in the Simple Tracker application:
  MainActivity
    This activity is started once the app is launched and contains many of the important features of the application. It obtains the user's username and ensures that location services are enabled. The activity contains five buttons and several text fields, which display the GPS coordinates and the time at which those values were most recently updated. The buttons are Start Trip, End Trip, Upload Trip, History, and Exit. In MainActivity, the app constantly records the device's GPS coordinates, but doesn't start recording them until the user hits Start Trip. End Trip simply stops saving the data to the file. Upload Trip launches UploadActivity. History launches HistoryActivity. Exit closes the application.
  UploadActivity
    This activity contains a list of all trips found on the phone. Clicking one presents the user with a prompt, making sure the user selected the correct trip, and uploads the file to the server upon user confirmation. The trip also contains a Return button, which brings the user back to MainActivity, but pressing the device's back button works, as well.
  MapsActivity
    This activity is launched from within HistoryActivity and simply loads a GoogleMap. It reads the file created for the given trip and places a pin on the map for each location in the file.
  HistoryActivity
    This activity is similar to UploadActivity, displaying a list of all trips found on the phone, as well as two buttons: Delete Trip and Return. Return is fairly self-explanatory, simply returning the user to MainActivity. Delete Trip creates a prompt, requiring the user to completely type out the trip they wish to delete. Selecting the trips in this activity launches MapActivity.
