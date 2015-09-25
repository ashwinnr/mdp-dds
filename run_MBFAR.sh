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
set inst = INSTANCE_FILENAME
set time_out_Mins = TIMEOUT_MINS
set job_title = JOB_NAME
set disc = USE_DISCOUNTING
set apricodd = USE_APRICODD
set apricodd_error = APRICODD_EPSILON
set naive_constraints = CONSTRAIN_NAIVELY
set apricodd_type = LOWER
set initial_value = ZERO
set bigdd = BIG_LONG_DD


rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

mkdir /tmp/$job_title\_tmp
cp -f /nfs/stak/students/n/nadamuna/mdp-dds/dist/MBFAR.jar /tmp/$job_title\_tmp/MBFAR.jar
cp -f $dom /tmp/$job_title\_tmp/$job_title\_dom
cp -f $inst /tmp/$job_title\_tmp/$job_title\_inst

hostname
date
limit

echo " solving $dom $inst"

time /usr/local/common64/jdk1.7.0_45/bin/java -jar -Xmx4g -Xms1g -ea /tmp/$job_title\_tmp/MBFAR.jar /tmp/$job_title\_tmp/$job_title\_dom /tmp/$job_title\_tmp/$job_title\_inst 0.1 42 $disc 1 30 $naive_constraints $bigdd $apricodd $apricodd_error $apricodd_type $initial_value $time_out_Mins RDDL 1 >& /tmp/$job_title\_tmp/$job_title

cp -f /tmp/$job_title\_tmp/$job_title /nfs/stak/students/n/nadamuna/mdp-dds/skill/
cp -f JOB_NAME.out        /nfs/stak/students/n/nadamuna/mdp-dds/skill/
cp -f JOB_NAME.err /nfs/stak/students/n/nadamuna/mdp-dds/skill/

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

echo "SPUDDFAR $dom $inst"

