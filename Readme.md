Example HC5 server that does not appear to work, ways to reproduce the problem: 

1. mvn build and run Main class (this should run on anything JDK8 and older)
1. Run apache bench (ab) against it, for example: 

```text
$ ab -n 1000 http://localhost:8080/
This is ApacheBench, Version 2.3 <$Revision: 1826891 $>
Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
Licensed to The Apache Software Foundation, http://www.apache.org/

Benchmarking localhost (be patient)
apr_socket_recv: Connection reset by peer (54)

```