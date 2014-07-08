#!/bin/bash
# <orders.log> <delay> <boolean: print-order-book>
#
# 1. ensure orders.log is in this format:
#  order_created {"price": "618.23", "amount": "0.05000000", "datetime": "1404815436", "id": 28971602, "order_type": 1}
#  order_deleted {"price": "611.01", "amount": "38.21077354", "datetime": "1404815383", "id": 28971526, "order_type": 0}
#  order_changed {"price": "618.25", "amount": "0.12000000", "datetime": "1404815403", "id": 28971550, "order_type": 0}
# 2. example order book reconstruction:
#  ./replay.sh orders.log 25 true 2>/dev/null
# will replay order book with a 25ms delay between order events.
# redirect 2>replay.log for indicator reconstruction. 

java -cp target/ob-jar-with-dependencies.jar:. net.parasec.ob.ReplayOrderBook $1 $2 $3

