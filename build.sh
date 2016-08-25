#!/bin/bash

# build the package
mvn clean package

# create zipped file to distribute the plugin
cd target
zip -r rnn-forecaster.zip pentaho-dl4j-plugin.jar lib

