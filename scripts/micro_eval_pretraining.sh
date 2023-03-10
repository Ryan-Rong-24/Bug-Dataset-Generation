#!/usr/bin/env bash

function prompt() {
    echo "Syntax: bash evaluate_pretrain.sh <GPU_ID> <EXPERIMENT_NAME> <CHECKPOINT_NUMBER> <DATA_NAME>";
    exit;
}

while getopts ":h" option; do
    case $option in
        h) # display help
          prompt;
    esac
done

if [[ $# -lt 1 ]]; then
    prompt;
fi

GPU=$1;
EXPERIMENT_NAME=${2:-"full-train"};
CHECKPOINT_NUMBER=${3:-"1000"};
DATA_NAME=${4:-"java_transformed"};
CODE_HOME_DIR='.';
DATA_DIR="${CODE_HOME_DIR}/resources/pretraining/data/${DATA_NAME}";

function evaluate() {
    OUTPUT_DIR="${CODE_HOME_DIR}/resources/pretraining/models/evaluation/${EXPERIMENT_NAME}-${DATA_NAME}-${CHECKPOINT_NUMBER}";
    mkdir -p ${OUTPUT_DIR};
    LOG="${OUTPUT_DIR}/pretraining-micro-eval.log";
    SUMMARY_DIR="${OUTPUT_DIR}/summary";
    mkdir -p ${SUMMARY_DIR};
    CACHE_DIR="${OUTPUT_DIR}/cache";
    mkdir -p ${CACHE_DIR};
    RES_DIR="${OUTPUT_DIR}/results";
    mkdir -p $RES_DIR;
    PRETEAINED_MODEL_BASE="${CODE_HOME_DIR}/resources/pretraining/models/";
    PRETRAINED_MODEL_PATH="${PRETEAINED_MODEL_BASE}/${EXPERIMENT_NAME}/checkpoint-${CHECKPOINT_NUMBER}";
    if [[ $CHECKPOINT_NUMBER == "0" ]]; then
	PRETRAINED_MODEL_PATH="Salesforce/codet5-base";
    fi
    export PYTHONIOENCODING=utf-8;
    export PYTHONPATH=$PYTHONPATH:$CODE_HOME_DIR;
    SCRIPT_PATH="${CODE_HOME_DIR}/src/pretraining/pretrain_evaluation.py";
    export CUDA_VISIBLE_DEVICES=${GPU};
    BATCH_SIZE=3;
    GRADIENT_ACCUM_STEP=1;
    SRC_LEN=512;
    TGT_LEN=512;
    echo $PRETRAINED_MODEL_PATH;

    python3 $SCRIPT_PATH \
            --task eval_pretrain --sub_task none \
            --model_type codet5 \
            --tokenizer_name ${PRETRAINED_MODEL_PATH}  \
            --model_name_or_path ${PRETRAINED_MODEL_PATH} \
            --data_dir ${DATA_DIR}  \
            --cache_path ${CACHE_DIR}  \
            --output_dir ${OUTPUT_DIR}  \
            --summary_dir ${SUMMARY_DIR} \
            --res_dir ${RES_DIR} \
            --res_fn ${RES_DIR}/results.txt \
            --eval_batch_size ${BATCH_SIZE} \
            --max_source_length ${SRC_LEN} \
            --max_target_length ${TGT_LEN} 2>&1 | tee ${LOG}
}

evaluate;
