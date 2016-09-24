#!/bin/bash

source $(dirname $0)/env/bin/activate
python $(dirname $0)/hybrid_train.py $*
ret=$?
deactivate
exit $ret
