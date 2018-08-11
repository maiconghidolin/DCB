# periodic-and-probabilistic-checkpoints
# LIMITATION OF CHECKPOINTS STORAGE SPACE IN AN OPTIMISTIC DISTRIBUTED SIMULATION

DCB is a prototype of a backbone to distributed and heterogeneous discrete event simulations. It's designed to support execution of simulation models divided into different nodes of a network. A behavior model can be divided into chunks and distributed among the nodes. Both updating model and synchronising barrier is performed in a distributed way. 

This code has been used by undergraduate students to study the architecture of this kind of system, considering subjects like lookahead, GVT and checkpointing algorithms. The actual version of this code has a simple network message passing system implemented where the frequency of message exchange is synthetic and do not represent any real system behavior. 

You should go for yourself or mail me if you want to learn how to build different models. 

# 1 - Compilation

Be sure you are using java version 1.8

At a linux terminal, navigate to the created repository and run $ make to compile the project. 

If it doesn’t work, try:

> $ mkdir bin/
> $ javac  -encoding ISO-8859-1 src/*.java -d bin/


# 2 - Run the simulation

Type $run to start the simulation. If it doesn’t work for you, check if you have xterm installed on your system. 
At this point, you should have five different DCB instances running on your computer. If that is the case, jump to the section 3.

If you do not have DCB running, try running each of the chats <chat_name> manually with command: 


> $ java -classpath bin/ DCBMThread <chat_name>.xml


Run each of the chats in different terminal instances. Chat5 must be the last one (He triggers the simulation start).


# 3 -Developers

 Note that if you want to change the behavior of the simulation model you must do it manually. 

To change total simulation time, search for the attribute simulationTime in the GVT.java file;
The MensagensAutomaticas class in Chat.java file manage the synthetic model rules to exchange messages;
The network configuration is made manually in the .xml files;
Please, note that there are no fedgvt in this version. Zanuzzo’s version have my modifications and includes fedgvt. I encourage you to look for that version. 


There’s a need of a simple tutorial to explain how to edit the .xml files.
There’s also a need for a tutorial that explains how to increase the number of components and how to change their behavior


Hope it helps. Mail me if you still have problems.
