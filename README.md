# Sensor statistics

## Application
Application counts statistics for the sensors in the given path. 

If path was not given as a parameter it will count the statistics for the example files located in the `src/main/resources`

## Running
Run it by typing:
```
sbt "run some/example/path/"
```

## Notable mentions
I used akka-stream + alpakka stack to implement this so memory consumption + scaling should be reasonable.


