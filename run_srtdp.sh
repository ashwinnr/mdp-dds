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
set type = UPPER 
set nstates = NUM_STATE
set nrounds = NUM_ROUNDS
set inst = INSTANCE_FILENAME
set pruning = PRUNING_ON
set numTraj = NUM_TRAJECTORIES
set generalize = GENERALIZATION_ON
set generalization_type = GENERALIZATION_TYPE
set path_type = PATH_TYPE
set consistency_type = CONSISTENCY_TYPE
set num_objects = NUM_OBJECTS
set lookahead = LOOKAHEAD_DEPTH
set on_policy = ON_POLICY_DEPTH
set do_apricodd = DO_APRICODD
set apricodd_error = APRICODD_ERROR
set time_out_Mins = TIMEOUT_MINS
set job_title = JOB_NAME
set xion = XION_DO
set globalV = STAT_V 
set globalinit = GLOBAL_INIT
set localinit = LOCAL_INIT
set truncatetrials = TRUNCATE_TRIALS
set mark_visited = MARKVISITED
set mark_solved = MARKSOLVED
set init_reward = INITREWARD
set remember_mode = REMEMBERMODE
set learning_mode = HOWTOLEARN

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

mkdir /tmp/$job_title\_tmp
cp -f /nfs/stak/students/n/nadamuna/mdp-dds/dist/sRTDP.jar /tmp/$job_title\_tmp/sRTDP.jar
cp -f $dom /tmp/$job_title\_tmp/$job_title\_dom
cp -f $inst /tmp/$job_title\_tmp/$job_title\_inst

hostname
date
limit

echo " solving $dom $inst"

time /usr/local/common64/jdk1.7.0_45/bin/java -jar -Xmx4g -Xms1g /tmp/$job_title\_tmp/sRTDP.jar -discounting true -testStates $nstates -testRounds $nrounds -domain /tmp/$job_title\_tmp/$job_title\_dom -instance /tmp/$job_title\_tmp/$job_title\_inst -seed 42 -actionVars true -constraintPruning $pruning -doApricodd $do_apricodd -apricoddError $apricodd_error -apricoddType UPPER -initialStateConf RDDL           -initialStateProb 0 -numTrajectories $numTraj -timeOutMins $time_out_Mins -stepsLookahead $lookahead -generalizeStates $generalize  -generalizeActions true -limitGeneralizedStates  -1 -limitGeneralizedActions -1 -generalization $generalization_type  -generalizationRule  $path_type -consistencyRule $consistency_type -convergenceTest 0.1 -onPolicyDepth $on_policy -learningRule DECISION_LIST -maxRules 100 -learningMode $learning_mode  -do_Xion $xion -stat_vfn $globalV -truncate_trials $truncatetrials -global_init $globalinit -local_init $localinit -mark_visited $mark_visited -mark_solved $mark_solved -remember_mode $remember_mode -init_reward $init_reward >& /tmp/$job_title\_tmp/$job_title

cp -f /tmp/$job_title\_tmp/$job_title /nfs/stak/students/n/nadamuna/mdp-dds/skill/
cp -f JOB_NAME.out        /nfs/stak/students/n/nadamuna/mdp-dds/skill/
cp -f JOB_NAME.err /nfs/stak/students/n/nadamuna/mdp-dds/skill/

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

echo "SRTDP completed $dom $inst"

