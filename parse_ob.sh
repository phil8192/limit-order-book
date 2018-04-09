#!/bin/bash
# <input.log> <output.csv>

headers() {
  t=$1
  p=$2
  for((i=1;i<=p;i++)) ;do 
    echo -n ${t}_percentile_vwap_${i},${t}_percentile_orders_${i},${t}_percentile_priceLevel_${i},${t}_percentile_volume_${i},
  done
}

#p=$(grep PERCENTILE_STEPS src/main/java/net/parasec/ob/Percentile.java |awk -F '= ' '{print $2}' |sed 's/;//')
p=20

cat >$2 <<EOF
event,ts,last_trade_ts,last_trade_price,last_trade_volume,last_trade_buy_or_sell,last_trade_taker,last_trade_maker,active_buys,active_sells,outstanding_buy_volume,outstanding_sell_volume,buy_impact,sell_impact,last_100_mo_buys,last_100_mo_buy_volume,last_100_mo_buy_max_volume,last_100_mo_sell_volume,last_100_mo_sell_max_volume,last_100_trade_buys,last_100_trade_buy_volume,last_100_trade_buy_trade_max,last_100_trade_sell_volume,last_100_trade_sell_max_volume,best_bid_price,best_bid_volume,best_ask_price,best_ask_volume,
EOF

headers "bid" $p >>$2
headers "ask" $p >>$2

cat >> $2 <<EOF 
lowest_price,highest_price,total_bids,total_asks,total_bid_volume,total_ask_volume,last_100_cancelled_bids,last_100_cancelled_bid_volume,last_100_cancelled_bid_max_volume,last_100_cancelled_ask_volume,last_100_cancelled_ask_max_volume,total_mo_buy_vol,total_mo_buy,total_mo_sell_vol,total_mo_sell
EOF

cat >> $2 <<EOF
,b1,b2,b4,b8,b16,b32,b64,b128,b256,b512,b1024,s1,s2,s4,s8,s16,s32,s64,s128,s256,s512,s1024
EOF

cat $2 |tr -d "\n" >tmplalaal
mv tmplalaal $2

echo >>$2

cat $1 |grep "^state," |cut -f 2- -d ',' >>$2
