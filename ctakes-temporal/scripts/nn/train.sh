#!/bin/bash

source $(dirname $0)/env/bin/activate
python $(dirname $0)/train_and_package.py $*
ret=$?
deactivate
exit $ret
