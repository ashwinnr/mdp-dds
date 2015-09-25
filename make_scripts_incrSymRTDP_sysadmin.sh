NUM_STATE=1 
NUM_ROUNDS=30

for conc in 1 3 5 7 10 
do 
	NUM_OBJECTS=$nobj


	for timeOut in 0.1 0.3 0.5  
	do
		for lookahead in 4 
		do
			for prune_limit in 1                
			do

			JOB_NAME=sysadmin_star_10_$conc\_incrSrtdp_$prune_limit\_$nobj\_$timeOut\_$lookahead

			cp run_incr_srtdp.sh /tmp/academic/$JOB_NAME.sh
			sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/sysadmin_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/academic/$JOB_NAME.sh
			sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/academic/$JOB_NAME.sh
			sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/sysadmin_star_10_$conc.rddl/"   /tmp/academic/$JOB_NAME.sh
			sed -i "s/PRUNING_ON/false/"  /tmp/academic/$JOB_NAME.sh
			sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/academic/$JOB_NAME.sh
			sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/academic/$JOB_NAME.sh
			sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/academic/$JOB_NAME.sh
	
			sed -i "s/JOB_OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.out/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/JOB_OUTPUT_ERROR/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.err/" /tmp/academic/$JOB_NAME.sh	
			sed -i "s/OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/ON_POLICY_DEPTH/100/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/DO_APRICODD/true/" /tmp/academic/$JOB_NAME.sh
	
			sed -i "s/APRICODD_ERROR/0.1/" /tmp/academic/$JOB_NAME.sh
			
			sed -i "s/INITIAL_STATE_DISTRIBUTION/RDDL/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/TRUNCATE_TRIALS/false/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/GLOBAL_INIT/VMAX/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/LOCAL_INIT/HRMAX/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/MARK_VISITED/true/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/MARK_SOLVED/false/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/REMEMBER_MODE/NONE/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/INIT_REWARD/false/" /tmp/academic/$JOB_NAME.sh
			sed -i "s/PRUNE_LIMIT/$prune_limit/" /tmp/academic/$JOB_NAME.sh
	
			echo $JOB_NAME 
			done				
		done
	done

#	for traj in 100     
#	do
#		for lookahead in 16
#		do
#			for prune_limit in 1e5 
#			do
#
#			JOB_NAME=academic_advising_incr_srtdp_$prune_limit\_$nobj\_$traj\_$lookahead
#
#			cp run_incr_srtdp.sh /tmp/academic/$JOB_NAME.sh
#			sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/academic/$JOB_NAME.sh
#			sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/academic/$JOB_NAME.sh
#			sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_inst_mdp__$nobj.rddl/"   /tmp/academic/$JOB_NAME.sh
#			sed -i "s/PRUNING_ON/false/"  /tmp/academic/$JOB_NAME.sh
#			sed -i "s/NUM_TRAJECTORIES/$traj/"   /tmp/academic/$JOB_NAME.sh
#			sed -i "s/TIMEOUT_MINS/-1/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/academic/$JOB_NAME.sh
#			sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/academic/$JOB_NAME.sh
#	
#			sed -i "s/JOB_OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.out/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/JOB_OUTPUT_ERROR/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.err/" /tmp/academic/$JOB_NAME.sh	
#			sed -i "s/OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/ON_POLICY_DEPTH/100/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/DO_APRICODD/true/" /tmp/academic/$JOB_NAME.sh
#	
#			sed -i "s/APRICODD_ERROR/0.1/" /tmp/academic/$JOB_NAME.sh
#			
#			sed -i "s/INITIAL_STATE_DISTRIBUTION/RDDL/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/TRUNCATE_TRIALS/false/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/GLOBAL_INIT/VMAX/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/LOCAL_INIT/HRMAX/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/MARK_VISITED/true/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/MARK_SOLVED/false/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/REMEMBER_MODE/NONE/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/INIT_REWARD/false/" /tmp/academic/$JOB_NAME.sh
#			sed -i "s/PRUNE_LIMIT/$prune_limit/" /tmp/academic/$JOB_NAME.sh
#	
#			echo $JOB_NAME 
#			done				
#		done
#	done
#
done
