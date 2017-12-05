#!/bin/bash

nohup sbt "compile" &
nohup sbt "runMain stresser.remoting.MUDStress" &
nohup ssh eherbert@pandora01.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote &' &
nohup ssh eherbert@pandora02.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote &' &
nohup ssh eherbert@pandora03.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote &' &
nohup ssh eherbert@pandora04.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote &' &
nohup ssh eherbert@pandora05.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote' &
nohup ssh eherbert@pandora06.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote' &
nohup ssh eherbert@pandora07.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote' &
nohup ssh eherbert@pandora08.cs.trinity.edu 'nohup java -cp /users/eherbert/CS/TA/CSCI1321-F17/CS2MUDTester/target/scala-2.12/CSCI1321MUDTester-assembly-1.0.jar stresser.remoting.StartupRemote' &
