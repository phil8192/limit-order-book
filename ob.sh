#!/bin/bash
#cd ..
#javac -cp lib/http/lib/httpclient-4.3.1.jar:lib/http/lib/httpcore-4.3.jar:lib/http/lib/commons-logging-1.1.3.jar:lib/pusher-java-client-0.2.2-jar-with-dependencies.jar:lib/gson-2.2.4.jar:lib/apache-log4j-1.2.17/log4j-1.2.17.jar:src:. src/*.java
#java -ea -cp lib/http/lib/httpclient-4.3.1.jar:lib/http/lib/httpcore-4.3.jar:lib/http/lib/commons-logging-1.1.3.jar:lib/pusher-java-client-0.2.2-jar-with-dependencies.jar:lib/gson-2.2.4.jar:lib/apache-log4j-1.2.17/log4j-1.2.17.jar:src:. OrderBookStream 
#cd -

java -jar target/ob-jar-with-dependencies.jar

