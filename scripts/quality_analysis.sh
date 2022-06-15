#!/bin/bash

PROJECT=${1}
SELECTOR1=${2}
SELECTOR2=${3}
STMT_COVERAGE=${4}
WORKDIR="."

SETTING_PROPERTIES_FILE="$WORKDIR/utbot-analytics/src/main/resources/config.properties"
touch $SETTING_PROPERTIES_FILE
echo "project=$PROJECT" > "$SETTING_PROPERTIES_FILE"
echo "selector1=$SELECTOR1" >> "$SETTING_PROPERTIES_FILE"
echo "selector2=$SELECTOR2" >> "$SETTING_PROPERTIES_FILE"

JAR_TYPE="utbot-analytics-1.0-SNAPSHOT.jar"
echo "JAR_TYPE: $JAR_TYPE"
UTBOT_JAR=$(ls -l utbot-analytics/build/libs/$JAR_TYPE | awk '{print $9}')
echo $UTBOT_JAR
MAIN_CLASS="com.huawei.utbot.QualityAnalysisKt"

if [[ -n $STMT_COVERAGE ]]; then
    MAIN_CLASS="com.huawei.utbot.StmtCoverageReportKt"
fi



#Running the jar
COMMAND_LINE="java $JVM_OPTS -cp $UTBOT_JAR $MAIN_CLASS"

$COMMAND_LINE
