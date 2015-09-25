NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 1 2 3 4            
do 
	NUM_OBJECTS=$nobj

	for timeOut in 0.1 0.3 0.5 
	do
		for generalize in true false  
		do
			for generalization_type in value  action 
			do
				for path_type in PATH 
				do
					for consistency_type in NONE
					do
						for Xion in true 
						do
							for lookahead in 4 
                                                        do
								JOB_NAME=tamarisk_hrmax_$nobj\_$lookahead\_10000_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
								cp run_srtdp.sh /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/tamarisk_mdp.rddl/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/tamarisk_inst_mdp__$nobj.rddl/"   /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/PRUNING_ON/false/"  /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/tamarisk/$JOB_NAME.sh
	
								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/tamarisk/$JOB_NAME.sh
	
								sed -i "s/DO_APRICODD/true/" /tmp/tamarisk/$JOB_NAME.sh
	
								sed -i "s/APRICODD_ERROR/0.1/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/XION_DO/$Xion/" /tmp/tamarisk/$JOB_NAME.sh
	
								sed -i "s/STAT_V/false/" /tmp/tamarisk/$JOB_NAME.sh
	
								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/tamarisk/$JOB_NAME.sh	
								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/MARKVISITED/true/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/MARKSOLVED/false/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/INITREWARD/false/" /tmp/tamarisk/$JOB_NAME.sh
								sed -i "s/REMEMBERMODE/NONE/" /tmp/tamarisk/$JOB_NAME.sh
								echo $JOB_NAME 
						
								if [ $generalize == false ]
								then
									break
								fi	
							done
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

#	for nTraj in 100
#	do
#		for generalize in false true 
#		do
#			for generalization_type in value  action 
#			do
#				for path_type in PATH 
#				do
#					for consistency_type in NONE
#					do
#						for Xion in true 
#						do
#							for lookahead in 8 16 32
#                                                        do
#								JOB_NAME=tamarisk_hrmax_$nobj\_$lookahead\_$nTraj\_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
#								cp run_srtdp.sh /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/tamarisk_mdp.rddl/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/tamarisk_inst_mdp__$nobj.rddl/"   /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/PRUNING_ON/false/"  /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/NUM_TRAJECTORIES/100/"   /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/TIMEOUT_MINS/-1/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/tamarisk/$JOB_NAME.sh
#
#								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/tamarisk/$JOB_NAME.sh
#	
#								sed -i "s/DO_APRICODD/true/" /tmp/tamarisk/$JOB_NAME.sh
#	
#								sed -i "s/APRICODD_ERROR/0.1/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/XION_DO/$Xion/" /tmp/tamarisk/$JOB_NAME.sh
#	
#								sed -i "s/STAT_V/false/" /tmp/tamarisk/$JOB_NAME.sh
#	
#								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/tamarisk/$JOB_NAME.sh	
#								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/MARKVISITED/true/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/MARKSOLVED/false/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/INITREWARD/false/" /tmp/tamarisk/$JOB_NAME.sh
#								sed -i "s/REMEMBERMODE/NONE/" /tmp/tamarisk/$JOB_NAME.sh
#								echo $JOB_NAME 
#						
#								if [ $generalize == false ]
#								then
#									break
#								fi	
#							done
#						done  
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
