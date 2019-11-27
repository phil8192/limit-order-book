#!/bin/bash
# <real_time> <symbol>
console_height=$(tput lines)
java -jar target/ob-jar-with-dependencies.jar "$console_height" "$@"