#!/bin/bash

TIME_LIMIT=${1}
ITERATIONS=${2}
OUTPUT_DIR=${3}
PYTHON_COMMAND=${4}

WORKDIR="."
$WORKDIR/scripts/train_data.sh $TIME_LIMIT
for (( i=0; i < $ITERATIONS; i++ ))
do
  $PYTHON_COMMAND $WORKDIR/scripts/train.py --features_dir $WORKDIR/eval/features --output_dir $OUTPUT_DIR/$i --prog_list $WORKDIR/scripts/prog_list
  while read prog; do
    prog="${prog%%[[:cntrl:]]}"
    $WORKDIR/scripts/run_contest_estimator.sh $prog $TIME_LIMIT "NN_REWARD_GUIDED_SELECTOR $OUTPUT_DIR/$i" "true eval/features/NN_REWARD_GUIDED_SELECTOR$i/$prog"
  done <"$WORKDIR/scripts/prog_list"
done
