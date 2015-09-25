NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 1 2 3 6 7 8           
do 
	NUM_OBJECTS=$nobj

	for timeOut in 0.1 
	do
		for generalize in true 
		do
			for generalization_type in value  action 
			do
				for path_type in PATH 
				do
					for consistency_type in NONE
					do
						for Xion in true 
						do
							for lookahead in 8 16 32 
                                                        do
								JOB_NAME=triangle_tireworld_hrmax_$nobj\_$lookahead\_10000_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
								cp run_srtdp.sh /tmp/triangle/$JOB_NAME.sh
								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/triangle_tireworld_mdp.rddl/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/triangle_tireworld_inst_mdp__$nobj.rddl/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/PRUNING_ON/false/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/triangle/$JOB_NAME.sh
	
								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/triangle/$JOB_NAME.sh

								sed -i "s/DO_APRICODD/true/" /tmp/triangle/$JOB_NAME.sh
		
								sed -i "s/APRICODD_ERROR/0.1/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/XION_DO/$Xion/" /tmp/triangle/$JOB_NAME.sh

								sed -i "s/STAT_V/false/" /tmp/triangle/$JOB_NAME.sh
		
								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/triangle/$JOB_NAME.sh	
								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/MARKVISITED/true/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/MARKSOLVED/false/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/INITREWARD/false/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/REMEMBERMODE/NONE/" /tmp/triangle/$JOB_NAME.sh
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

	for nTraj in 100
	do
		for generalize in false true 
		do
			for generalization_type in value  action 
			do
				for path_type in PATH 
				do
					for consistency_type in NONE
					do
						for Xion in true 
						do
							for lookahead in 8 16 32
                                                        do
								JOB_NAME=triangle_tireworld_hrmax_$nobj\_$lookahead\_$nTraj\_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
								cp run_srtdp.sh /tmp/triangle/$JOB_NAME.sh
								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/triangle_tireworld_mdp.rddl/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/triangle_tireworld_inst_mdp__$nobj.rddl/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/PRUNING_ON/false/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_TRAJECTORIES/100/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/TIMEOUT_MINS/-1/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/triangle/$JOB_NAME.sh
								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/triangle/$JOB_NAME.sh
								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/triangle/$JOB_NAME.sh

								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/triangle/$JOB_NAME.sh
	
								sed -i "s/DO_APRICODD/true/" /tmp/triangle/$JOB_NAME.sh
	
								sed -i "s/APRICODD_ERROR/0.1/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/XION_DO/$Xion/" /tmp/triangle/$JOB_NAME.sh
	
								sed -i "s/STAT_V/false/" /tmp/triangle/$JOB_NAME.sh
	
								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/triangle/$JOB_NAME.sh	
								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/MARKVISITED/true/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/MARKSOLVED/false/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/INITREWARD/true/" /tmp/triangle/$JOB_NAME.sh
								sed -i "s/REMEMBERMODE/NONE/" /tmp/triangle/$JOB_NAME.sh
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

done
