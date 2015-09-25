NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 1 2 3 4 5 6 7 8 9 10 
do
	NUM_OBJECTS=$nobj

#	for timeOut in 0.1 0.3 0.5
#	do
#		JOB_NAME=academic_advising_table_$nobj\_10000_$timeOut 
#		cp run_tableRTDP.sh /tmp/academic/$JOB_NAME.sh
#		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
#		sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/academic/$JOB_NAME.sh
#		sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/academic/$JOB_NAME.sh
#		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_inst_mdp__$nobj.rddl/"   /tmp/academic/$JOB_NAME.sh
#		sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/academic/$JOB_NAME.sh
#		sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/academic/$JOB_NAME.sh
#		sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/academic/$JOB_NAME.sh
#		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
#		sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/academic/$JOB_NAME.sh
#		echo $JOB_NAME 
#						
#	done

	for numTraj in 100 
	do
		JOB_NAME=academic_advising_table_$nobj\_$numTraj
		cp run_tableRTDP.sh /tmp/academic/$JOB_NAME.sh
		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
		sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/academic/$JOB_NAME.sh
		sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/academic/$JOB_NAME.sh
		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_inst_mdp__$nobj.rddl/"   /tmp/academic/$JOB_NAME.sh
		sed -i "s/NUM_TRAJECTORIES/$numTraj/"   /tmp/academic/$JOB_NAME.sh
		sed -i "s/TIMEOUT_MINS/-1/" /tmp/academic/$JOB_NAME.sh
		sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/academic/$JOB_NAME.sh
		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
		sed -i "s/LOOKAHEAD_DEPTH/16/" /tmp/academic/$JOB_NAME.sh

		echo $JOB_NAME 
	
	done
done
