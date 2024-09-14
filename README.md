# realestate-cloudconnector-agent
Pipe observations from Building Automation Systems to your favorite Cloud Provider. Gain insights into the environmental impact of your Commercial RealEstate.

> Info:
> 
> This repository is work in progress and will lack documentation for a month or two.
> Please contact me, Bård at [baardl](https://github.com/baardl) if you would like to test this functionallity or have any questions.
> 
>
## Status

* [Health](http://localhost:8083/cloudconnector/health)
* [SensorIdRepository](http://localhost:8083/cloudconnector/repository/sensorids)

## Configuration
``` 
sensormappings.simulator.enabled=true
```

## Import SensorIds
### From CSV File
Parse sensor ids from a csv file
* Expected format:
* SensorId;SensorSystem;Identificator
* Identificator is either
*  1 a single value or
*  2 a key-value pair separated by ":"
*  3 multiple key-value pairs separated by ";"'

## Statistics and Metrics

[Metrics](http://localhost:8083/admin/metrics/app/*)
