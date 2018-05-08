# EthMonitor
A gui to monitor the api port of a Ethminer instance.

**This is the early stages of development. There maybe very drastic changes, issues will not be addressed until a release is generated. Any ideas, comments, or suggestions can be made via inline comments in a commit, a pull request, or issue tracker.**

![screen](https://i.imgur.com/4hxNEIJ.png)



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

Point EthMonitor to your server by adding lines to you initialization file (config.ini).
If you do not have a config.ini one will be generated for you on start up.
```
#IPaddress and port of the server to poll, more than one server line can be added
#Example: server={ipaddress}:{port}
server=127.0.0.1:3333
#Poling rate, amount of time in ms to wait between poles
poling_rate=1000
#Graphing Points (default:100)
graph_points=100
#Verbosity of the console, 1=TX/RX info, 2=ResponseParsing
verbose=0
#Animate gauges, 1=true (default), 0=false
animate=false
#Enable Tray Icon, 1=true (default), 0=false
trayicon=true
```


