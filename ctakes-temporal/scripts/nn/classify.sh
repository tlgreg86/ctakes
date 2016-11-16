#!/bin/bash

source $(dirname $0)/env/bin/activate
python $(dirname $0)/cnn_classify_hybrid.py $*
ret=$?
deactivate
exit $ret
