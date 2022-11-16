# OpenCV-PnP (base on [OpenCV](https://github.com/openpnp/opencv))

[OpenCV](http://opencv.org) Java bindings packaged with native libraries, seamlessly delivered as a turn-key Maven dependency.

## Usage

### Project

OpenCV-PnPnCV use Maven to publish Java packages to a registry maven artifact.

#### [Maven](http://maven.apache.org/)
```xml
  <!-- ... -->

  <repositories>
    <repository>
      <id>opencv</id>
      <url>https://raw.githubusercontent.com/antuking/opencv-pnp/mvn-artifact</url>
      <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
      </snapshots>
    </repository>
  </repositories>
  
  <!-- ... -->
```
```xml
  <!-- ... -->

  <dependencies>
    <dependency>
      <groupId>io.kyzu</groupId>
      <artifactId>opencv-pnp</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
  
  <!-- ... -->
```
