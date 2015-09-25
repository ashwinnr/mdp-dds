NUM_STATE=1 
NUM_ROUNDS=30

for nobj in 2 4 5 6 7 8 9 10 
do 
	NUM_OBJECTS=$nobj

#	for timeOut in 0.1 0.3 0.5 
#	do
#		for generalize in true 
#		do
#			for generalization_type in value  
#			do
#				for path_type in ALL_PATHS
#				do
#					for consistency_type in NONE
#					do
#
#							JOB_NAME=academic_advising_old_srtdp_$nobj\_10000_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
#							cp run_old_srtdp.sh /tmp/academic/$JOB_NAME.sh
#							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
#							sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/academic/$JOB_NAME.sh
#							sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/academic/$JOB_NAME.sh
#							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_inst_mdp__$nobj.rddl/"   /tmp/academic/$JOB_NAME.sh
#							sed -i "s/PRUNING_ON/false/"  /tmp/academic/$JOB_NAME.sh
#							sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/academic/$JOB_NAME.sh
#							sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/academic/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/academic/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/academic/$JOB_NAME.sh
#							sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/academic/$JOB_NAME.sh
#							sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/academic/$JOB_NAME.sh
#							sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/academic/$JOB_NAME.sh
#							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
#							sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/academic/$JOB_NAME.sh
#							sed -i "s/DO_APRICODD/true/" /tmp/academic/$JOB_NAME.sh
#							sed -i "s/APRICODD_ERROR/0.1/" /tmp/academic/$JOB_NAME.sh
#							echo $JOB_NAME 
#						
#					done
#				done
#			done
#		done
#	done

	for numTraj in 100                  
	do
		for generalize in true 
		do
			for generalization_type in value               
			do
				for path_type in ALL_PATHS  
				do
					for consistency_type in NONE
					do
						for Xion in false
						do	
							JOB_NAME=academic_advising_old_srtdp_$nobj\_$numTraj\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
							cp run_old_srtdp.sh /tmp/academic/$JOB_NAME.sh
							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
							sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/academic/$JOB_NAME.sh
							sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/academic/$JOB_NAME.sh
							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_inst_mdp__$nobj.rddl/"   /tmp/academic/$JOB_NAME.sh
							sed -i "s/PRUNING_ON/false/"  /tmp/academic/$JOB_NAME.sh
							sed -i "s/NUM_TRAJECTORIES/$numTraj/"   /tmp/academic/$JOB_NAME.sh
							sed -i "s/TIMEOUT_MINS/-1/" /tmp/academic/$JOB_NAME.sh
							sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/academic/$JOB_NAME.sh
							sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/academic/$JOB_NAME.sh
							sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/academic/$JOB_NAME.sh
							sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/academic/$JOB_NAME.sh
							sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/academic/$JOB_NAME.sh
							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
							sed -i "s/LOOKAHEAD_DEPTH/16/" /tmp/academic/$JOB_NAME.sh

							sed -i "s/JOB_OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.out/" /tmp/academic/$JOB_NAME.sh
							sed -i "s/JOB_OUTPUT_ERROR/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.err/" /tmp/academic/$JOB_NAME.sh	
							sed -i "s/OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
							sed -i "s/ON_POLICY_DEPTH/100/" /tmp/academic/$JOB_NAME.sh

							sed -i "s/DO_APRICODD/true/" /tmp/academic/$JOB_NAME.sh

							sed -i "s/APRICODD_ERROR/0.1/" /tmp/academic/$JOB_NAME.sh
							sed -i "s/XION_DO/$Xion/" /tmp/academic/$JOB_NAME.sh
							sed -i "s/STAT_V/false/" /tmp/academic/$JOB_NAME.sh

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
done
