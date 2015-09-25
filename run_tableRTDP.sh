#!/bin/csh
#
# use current working directory for input and output - defaults is 
# to use the users home directory
#$ -cwd
#
# name this job
#$ -N JOB_NAME
#
# send stdout and stderror to this file
#$ -o JOB_NAME.out
#$ -e JOB_NAME.err
#$ -j y
#
# select queue - if needed 
#$ -hard
#$ -l m_mem_free=6G
#$ -l h=compute-0*|compute-1*|compute-2*|compute-3*|compute-4*|compute-5*
set dom = DOMAIN_FILENAME
set nstates = NUM_STATE
set nrounds = NUM_ROUNDS
set inst = INSTANCE_FILENAME
set numTraj = NUM_TRAJECTORIES
set num_objects = NUM_OBJECTS
set lookahead = LOOKAHEAD_DEPTH
set time_out_Mins = TIMEOUT_MINS
set job_title = JOB_NAME

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

mkdir /tmp/$job_title\_tmp
rm -f /tmp/$job_title\_tmp/*
cp -f /nfs/stak/students/n/nadamuna/mdp-dds/dist/TableRTDP.jar /tmp/$job_title\_tmp/TableRTDP.jar
cp -f $dom /tmp/$job_title\_tmp/$job_title\_dom
cp -f $inst /tmp/$job_title\_tmp/$job_title\_inst

hostname
date
limit

echo " solving $dom $inst"

time /usr/local/common64/jdk1.7.0_45/bin/java -jar -Xmx4g -Xms1g /tmp/$job_title\_tmp/TableRTDP.jar -discounting true -testStates $nstates -testRounds $nrounds -domain /tmp/$job_title\_tmp/$job_title\_dom -instance /tmp/$job_title\_tmp/$job_title\_inst -seed 42 -actionVars true -initialStateConf RDDL           -initialStateProb 0 -numTrajectories $numTraj -timeOutMins $time_out_Mins -stepsLookahead $lookahead  >& /tmp/$job_title\_tmp/$job_title

cp -f /tmp/$job_title\_tmp/$job_title /nfs/stak/students/n/nadamuna/mdp-dds/skill/ 
cp -f JOB_NAME.out        /nfs/stak/students/n/nadamuna/mdp-dds/skill/ 
cp -f JOB_NAME.err /nfs/stak/students/n/nadamuna/mdp-dds/skill/ 

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

echo "SRTDP completed $dom $inst"

