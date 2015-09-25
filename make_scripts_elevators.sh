NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 1 3 5 
do
	NUM_OBJECTS=$nobj

	for timeOut in 0.1 0.3 0.5
	do
		for generalize in true false 
		do
			for generalization_type in value action 
			do
				for path_type in PATH  
				do
					for consistency_type in NONE
					do
						for Xion in true 
						do

							JOB_NAME=elevators_$nobj\_10000_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
							cp run_srtdp.sh /tmp/elevators/$JOB_NAME.sh
							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/elevators_mdp.rddl/" /tmp/elevators/$JOB_NAME.sh
							sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/elevators/$JOB_NAME.sh
							sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/elevators/$JOB_NAME.sh
							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/elevators_inst_mdp__$nobj.rddl/"   /tmp/elevators/$JOB_NAME.sh
							sed -i "s/PRUNING_ON/false/"  /tmp/elevators/$JOB_NAME.sh
							sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/elevators/$JOB_NAME.sh
							sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/elevators/$JOB_NAME.sh
							sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/elevators/$JOB_NAME.sh
							sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/elevators/$JOB_NAME.sh
							sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/elevators/$JOB_NAME.sh
							sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/elevators/$JOB_NAME.sh
							sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/elevators/$JOB_NAME.sh
							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/elevators/$JOB_NAME.sh
							sed -i "s/LOOKAHEAD_DEPTH/4/" /tmp/elevators/$JOB_NAME.sh

							sed -i "s/ON_POLICY_DEPTH/8/" /tmp/elevators/$JOB_NAME.sh

							sed -i "s/DO_APRICODD/true/" /tmp/elevators/$JOB_NAME.sh

							sed -i "s/APRICODD_ERROR/0.1/" /tmp/elevators/$JOB_NAME.sh
							sed -i "s/XION_DO/$Xion/" /tmp/elevators/$JOB_NAME.sh
							sed -i "s/STAT_V/false/" /tmp/elevators/$JOB_NAME.sh

							echo $JOB_NAME 
						
							if [ $generalize == false ]
							then
								break
							fi	
						done  
						if [ $generalize == false ]
							then
							break
						fi	
					done
					if [ $generalize == false ]
						then
						break
					fi	
				done
				if [ $generalize == false ]
					then
					break
				fi	
			done
		done
	done

#	for numTraj in 100 500 1000 2000
#	do
#		for generalize in true 
#		do
#			for generalization_type in action value reward
#			do
#				for path_type in PATH ALL_PATHS  
#				do
#					for consistency_type in NONE
#					do
#						for Xion in false
#						do	
#							JOB_NAME=crossing_traffic_$nobj\_$numTraj\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
#							cp run_srtdp.sh /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_$nobj\_$nobj.rddl/"   /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/PRUNING_ON/false/"  /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/NUM_TRAJECTORIES/$numTraj/"   /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/TIMEOUT_MINS/-1/" /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/elevators/$JOB_NAME.sh
#
#							sed -i "s/JOB_OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/crossing_log_v5\/$JOB_NAME.out/" /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/JOB_OUTPUT_ERROR/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/crossing_log_v5\/$JOB_NAME.err/" /tmp/elevators/$JOB_NAME.sh	
#							sed -i "s/OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/crossing_log_v5\/$JOB_NAME/" /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/ON_POLICY_DEPTH/8/" /tmp/elevators/$JOB_NAME.sh
#
#							sed -i "s/DO_APRICODD/true/" /tmp/elevators/$JOB_NAME.sh
#
#							sed -i "s/APRICODD_ERROR/0.1/" /tmp/elevators/$JOB_NAME.sh
#							sed -i "s/XION_DO/$Xion/" /tmp/elevators/$JOB_NAME.sh
#
#							echo $JOB_NAME 
#			
#							if [ $generalize == false ]
#								then 
#								break
#							fi
#						done
#						
#						if [ $generalize == false ]
#							then
#							break
#						fi	
#					done  
#					if [ $generalize == false ]
#						then
#						break
#					fi	
#				done
#				if [ $generalize == false ]
#					then
#					break
#				fi	
#			done
#		done
#	done
done
