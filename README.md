# EthMonitor
A gui to monitor the api port of a Ethminer instance.

**This is the early stages of development. There maybe very drastic changes, issues will not be addressed until a release is generated. Any ideas, comments, or suggestions can be made via inline comments in a commit, a pull request, or issue tracker.**

![screen](https://i.imgur.com/8iFAzj5.png)


![screen](https://i.imgur.com/eayAFX6.png)

## Compilation
*****

Using maven to handle dependencies.

* Install [Maven 3](http://maven.apache.org/download.html)
* Clone this repo and: `mvn clean install`

## Downloads
*****
[Downloads](https://github.com/deathmarine/EthMonitor/releases)

## Configuration
*****
First configure ethminer to open a port. Add the argument below to your startup script.
```
--api-port 3333
```
For example:
```
ethminer.exe -HWMON 1 -RH -G -P stratum+tcp://abcdefghijklmnopqrstuvwxyz1234567890.your_name_here@us1.ethermine.org:4444 --opencl-device 0 --api-port -3333
```
Optionally in linux you can run ethminer headless/nogui, with this gist.
[Headless Script](https://gist.github.com/deathmarine/f29f541318247b9066a00194da08ad2f)


Point EthMonitor to your server by adding lines to you initialization file (config.ini).
If you do not have a config.ini one will be generated for you on start up.
```
## Configuration ##
#IPaddress and port of the server to pole, more than one server line can be added
#Example: server={ipaddress}:{port}
server=127.0.0.1:3333

#Enable Tray Icon, true (default), false
trayicon=true
#Enable "AreYouSure" Question for exiting.
trayicon.question=true
#Detailed results, includes wattage
detailed=true

##   Appearance   ##
#Max hashrate, status gauge (default:200)
gauge_max.status=200
#Max hashrate, gpu gauge (default:50)
gauge_max.gpu=50
#Poling rate, amount of time in ms to wait between poles
poling_rate=1000
#Graphing Points (default:100)
graph_points=100
#Verbosity of the console, 1=TX/RX info, 2=ResponseParsing
verbose=0
#Animate gauges, true (default), false
animate=true

```


