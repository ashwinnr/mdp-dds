NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 1 2 3 4 5 6
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
							for lookahead in 4 
							do

								JOB_NAME=skill_teaching_hrmax_$nobj\_$lookahead\_10000_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
								cp run_srtdp.sh /tmp/skill/$JOB_NAME.sh
								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/skill_teaching_mdp.rddl/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/skill_teaching_inst_mdp__$nobj.rddl/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/PRUNING_ON/false/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/skill/$JOB_NAME.sh
								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/DO_APRICODD/true/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/APRICODD_ERROR/0.1/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/XION_DO/$Xion/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/STAT_V/false/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/skill/$JOB_NAME.sh
						
								sed -i "s/MARKVISITED/true/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/MARKSOLVED/false/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/INITREWARD/false/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/REMEMBERMODE/NONE/" /tmp/skill/$JOB_NAME.sh
	
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
				if [ $generalize == false ]
					then
					break
				fi	
			done
		done
	done

	for nTraj in 30 
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
							for lookahead in 4 
							do

								JOB_NAME=skill_teaching_hrmax_$nobj\_$lookahead\_$nTraj\_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
								cp run_srtdp.sh /tmp/skill/$JOB_NAME.sh
								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/skill_teaching_mdp.rddl/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/skill_teaching_inst_mdp__$nobj.rddl/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/PRUNING_ON/false/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_TRAJECTORIES/$nTraj/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/TIMEOUT_MINS/-1/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/skill/$JOB_NAME.sh
								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/skill/$JOB_NAME.sh
								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/skill/$JOB_NAME.sh
								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/DO_APRICODD/true/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/APRICODD_ERROR/0.1/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/XION_DO/$Xion/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/STAT_V/false/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/skill/$JOB_NAME.sh
	
								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/skill/$JOB_NAME.sh
						
								sed -i "s/MARKVISITED/true/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/MARKSOLVED/false/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/INITREWARD/false/" /tmp/skill/$JOB_NAME.sh
								sed -i "s/REMEMBERMODE/NONE/" /tmp/skill/$JOB_NAME.sh
	
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
				if [ $generalize == false ]
					then
					break
				fi	
			done
		done
	done


done
