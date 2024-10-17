#!/usr/bin/env bash

# Credit: https://gist.github.com/ankithooda/b0d624aec9b3ed2882713d59feba4b11

classpath=".:lib/*"

mkdir -p bin/server
mkdir -p bin/client

javac -cp $classpath -d bin/server simpledb/server/*

javac -cp $classpath -d bin/client simpleclient/SimpleIJ.java
javac -cp $classpath -d bin/client simpleclient/network/*
javac -cp $classpath -d bin/client simpleclient/embedded/*

echo "Done compiling!"