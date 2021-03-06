NUM_STATE=1 
NUM_ROUNDS=30

for nobj in  4       
do 
	NUM_OBJECTS=$nobj

	for timeOut in 0.5 
	do
		for generalize in true 
		do
			for generalization_type in value  
			do
				for path_type in ALL_PATHS
				do
					for consistency_type in NONE
					do

							JOB_NAME=crossing_traffic_oldsrtdp_$nobj\_10000_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
							cp run_old_srtdp.sh /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_inst_mdp__$nobj.rddl/"   /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/PRUNING_ON/false/"  /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/LOOKAHEAD_DEPTH/16/" /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/DO_APRICODD/true/" /tmp/crossing_v5/$JOB_NAME.sh
							sed -i "s/APRICODD_ERROR/0.1/" /tmp/crossing_v5/$JOB_NAME.sh
							echo $JOB_NAME 
						
					done
				done
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
#							JOB_NAME=academic_advising_$nobj\_$numTraj\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
#							cp run_srtdp.sh /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/academic_advising_inst_mdp__$nobj.rddl/"   /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/PRUNING_ON/false/"  /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/NUM_TRAJECTORIES/$numTraj/"   /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/TIMEOUT_MINS/-1/" /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/crossing_v5/$JOB_NAME.sh
#
#							sed -i "s/JOB_OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.out/" /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/JOB_OUTPUT_ERROR/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME.err/" /tmp/crossing_v5/$JOB_NAME.sh	
#							sed -i "s/OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/academic\/$JOB_NAME/" /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/ON_POLICY_DEPTH/8/" /tmp/crossing_v5/$JOB_NAME.sh
#
#							sed -i "s/DO_APRICODD/true/" /tmp/crossing_v5/$JOB_NAME.sh
#
#							sed -i "s/APRICODD_ERROR/0.1/" /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/XION_DO/$Xion/" /tmp/crossing_v5/$JOB_NAME.sh
#							sed -i "s/STAT_V/false/" /tmp/crossing_v5/$JOB_NAME.sh
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
