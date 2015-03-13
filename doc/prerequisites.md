# Installing Prerequisites on Ubuntu 14.04

This documents describes steps necessary for building and launching service broker on freshly installed Ubuntu 14.04.

## Oracle Java 8

```
sudo apt-add-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
```

Currently this installs jdk8u40.

## Gradle 2.3

```
$ sudo add-apt-repository ppa:cwchien/gradle
$ sudo apt-get update
$ sudo apt-get install gradle-2.3
```

## Build Brooklyn

```
$ git clone https://github.com/apache/incubator-brooklyn.git
$ cd incubator-brooklyn/
$ mvn clean install -DskipTests
```

__Now you should be able to build SB.__

## MongoDB

```
$ sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
$ echo "deb http://repo.mongodb.org/apt/ubuntu "$(lsb_release -sc)"/mongodb-org/3.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.0.list
$ sudo apt-get update
$ sudo apt-get install -y mongodb-org

$ tail /var/log/mongodb
...
[initandlisten] waiting for connections on port 27017
```

It takes around 1m for MongoDB to start for the first time. Last line in the log shows you have MongoDB up and accepting connections.

## Start Brooklyn

```
$ cd incubator-brooklyn/usage/dist/target/brooklyn-dist
$ bin/brooklyn launch
```

__Now you should be able to run SB.__

NOTE: To build and run Brooklyn 0.7.0-M2-incubating you need the older version of Java, Java 6. You can install it from the already configured PPA by issuing the command `sudo apt-get install oracle-java6-installer`.