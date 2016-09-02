#!/bin/bash

source $(dirname $0)/env/bin/activate
python $(dirname $0)/predict.py $*
ret=$?
deactivate
exit $ret
