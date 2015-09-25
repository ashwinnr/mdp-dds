NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 1 3 5
do
	NUM_OBJECTS=$nobj

	for timeOut in 0.1 0.3 0.5 
	do
		JOB_NAME=elevators_table_$nobj\_10000_$timeOut 
		cp run_tableRTDP.sh /tmp/elevators/$JOB_NAME.sh
		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/elevators_mdp.rddl/" /tmp/elevators/$JOB_NAME.sh
		sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/elevators/$JOB_NAME.sh
		sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/elevators/$JOB_NAME.sh
		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/elevators_inst_mdp__$nobj.rddl/"   /tmp/elevators/$JOB_NAME.sh
		sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/elevators/$JOB_NAME.sh
		sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/elevators/$JOB_NAME.sh
		sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/elevators/$JOB_NAME.sh
		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/elevators/$JOB_NAME.sh
		sed -i "s/LOOKAHEAD_DEPTH/4/" /tmp/elevators/$JOB_NAME.sh
		echo $JOB_NAME 
						
	done

#	for numTraj in 100 500 1000 2000
#	do
#		JOB_NAME=elevators_traffic_table_$nobj\_$numTraj
#		cp run_tableRTDP.sh /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/elevators_traffic_mdp.rddl/" /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/elevators_traffic_$nobj\_$nobj.rddl/"   /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/NUM_TRAJECTORIES/$numTraj/"   /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/TIMEOUT_MINS/-1/" /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/elevators/$JOB_NAME.sh
#		sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/elevators/$JOB_NAME.sh
#
#		echo $JOB_NAME 
#	
#	done
done
