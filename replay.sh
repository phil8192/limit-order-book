#!/bin/bash
# <orders.log> <delay> <boolean: print-order-book>

java -jar target/ob-jar-with-dependencies.jar net.parasec.ReplayOrderBook $1 $2 $3
