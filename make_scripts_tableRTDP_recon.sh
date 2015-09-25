NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 3 5 7
do
	NUM_OBJECTS=$nobj

	for timeOut in 0.7 0.9 1.5
	do
		JOB_NAME=recon_table_$nobj\_10000_$timeOut 
		cp run_tableRTDP.sh /tmp/skill/$JOB_NAME.sh
		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/recon_mdp.rddl/" /tmp/skill/$JOB_NAME.sh
		sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/skill/$JOB_NAME.sh
		sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/skill/$JOB_NAME.sh
		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/recon_inst_mdp__$nobj.rddl/"   /tmp/skill/$JOB_NAME.sh
		sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/skill/$JOB_NAME.sh
		sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/skill/$JOB_NAME.sh
		sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/skill/$JOB_NAME.sh
		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/skill/$JOB_NAME.sh
		sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/skill/$JOB_NAME.sh
		echo $JOB_NAME 
						
	done

#	for numTraj in 100 500 1000 2000
#	do
#		JOB_NAME=recon_traffic_table_$nobj\_$numTraj
#		cp run_tableRTDP.sh /tmp/skill/$JOB_NAME.sh
#		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/recon_traffic_mdp.rddl/" /tmp/skill/$JOB_NAME.sh
#		sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/skill/$JOB_NAME.sh
#		sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/skill/$JOB_NAME.sh
#		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/recon_traffic_$nobj\_$nobj.rddl/"   /tmp/skill/$JOB_NAME.sh
#		sed -i "s/NUM_TRAJECTORIES/$numTraj/"   /tmp/skill/$JOB_NAME.sh
#		sed -i "s/TIMEOUT_MINS/-1/" /tmp/skill/$JOB_NAME.sh
#		sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/skill/$JOB_NAME.sh
#		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/skill/$JOB_NAME.sh
#		sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/skill/$JOB_NAME.sh
#
#		echo $JOB_NAME 
#	
#	done
done
