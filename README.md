# HueSense
HueSense allows you to monitor all the Hue motion sensors that are connected to you Bridge. Each motion sensor provides
data for the current motion detection, the current light level and the current measured temperature.

![Main view](./docs/main-tumb.png?raw=true)

[Screenshot of main window](./docs/main.png?raw=true)

# Features

* View current motion, light level and temperature
* Display history graph of sensor data
* QuickView mode (small view of current data without main window)
* Save collected data in database
* Set an alarm if motion detected (COMING SOON?)


![Light sensor](./docs/light-tumb.png?raw=true)
![Motion sensor](./docs/motion-tumb.png?raw=true)
![QuickView](./docs/quickview.png?raw=true)

[Screenshot of light window](./docs/light.png?raw=true)

[Screenshot of motion window](./docs/motion.png?raw=true)

## Data collection
No data is sent to any remote server, your privacy is guaranteed! 


# Downloads

Download the [latest release](https://github.com/dainesch/HueSense/releases/download/v1.1/HueSense-1.1.jar) and run it.

* [Version 1.1 (latest release)](https://github.com/dainesch/HueSense/releases/download/v1.1/HueSense-1.1.jar)
* [Version 1.0](https://github.com/dainesch/HueSense/releases/download/v1.0/HueSense-1.0.jar)

[Java 8](https://java.com/en/download/) is required to run HueSense!

# Changelog

### Version 1.1

* Improve Charts
* Add database (Derby) to store collected data (located in %user.home%/.huesense/)
* Data export to CSV

### Version 1.0

* Initial Release
