# phone-gps
Kotlin application that allows you to use your phone as a GPS tracker. Launches a service to determine, store, display your location using GPS functions. Let you send your GPS location to your own server at a regular interval.

After starting location services, location data is collected and stored for 30 days.
Key features:
- determines geolocation by all available methods
- stores information for a specified time (by default 30 days)
- allows you to view the track for the specified date
- possible to send data to your personal server
- automatic stop service at the specified time (by default 18:00)
- there is an example of a server written in GO
