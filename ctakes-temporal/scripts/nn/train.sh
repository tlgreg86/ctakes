#!/bin/bash

source $(dirname $0)/env/bin/activate
python $(dirname $0)/lstm_train.py $*
ret=$?
deactivate
exit $ret
