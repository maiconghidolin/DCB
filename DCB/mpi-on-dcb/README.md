DCB is a prototype of a heterogeneous distributed simulation system. 
It's designed to support execution of a simulation model divided into different nodes of a network. The network configuration is made manually in the .xml files. 

Both updating model and synchronising barrier is performed in a distributed way. A model can be divided into chunks and distributed among
the nodes.

This code has been used by undergraduated students to study the archtecture of this kind of system, considering subjects like lookahead algorithms, gvt algorihtms and checkpointing algorithms.


The actual version of this code has a simple message passing system implemented where the frequency of message exchange is extremaly random. 
Also, there is no decent documentation. You should go for yourself or mail me if you want to learn how to build different models. 

If you wanna run the implemented model, just make and ./run. Maybe you do not undertand what really is going on and i'm not sure if i do.


