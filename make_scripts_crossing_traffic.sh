NUM_STATE=1 
NUM_ROUNDS=30 

for nobj in 5      
do
	NUM_OBJECTS=$nobj

	for timeOut in 0.5                       
	do
		for generalize in false 
		do
			for generalization_type in value                
			do
				for path_type in PATH           
				do
					for consistency_type in NONE
					do
						for Xion in true 
						do
							for lookahead in 16	
							do

								JOB_NAME=crossing_traffic_hrmax_$nobj\_$lookahead\_10000_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
								cp run_srtdp.sh /tmp/crossing/$JOB_NAME.sh
								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/crossing/$JOB_NAME.sh
								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/crossing/$JOB_NAME.sh
								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_inst_mdp__$nobj.rddl/"   /tmp/crossing/$JOB_NAME.sh
								sed -i "s/PRUNING_ON/false/"  /tmp/crossing/$JOB_NAME.sh
								sed -i "s/NUM_TRAJECTORIES/10000/"   /tmp/crossing/$JOB_NAME.sh
								sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/crossing/$JOB_NAME.sh
								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/crossing/$JOB_NAME.sh
								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/crossing/$JOB_NAME.sh
								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/crossing/$JOB_NAME.sh
								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/crossing/$JOB_NAME.sh
								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/crossing/$JOB_NAME.sh

								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/crossing/$JOB_NAME.sh

								sed -i "s/DO_APRICODD/true/" /tmp/crossing/$JOB_NAME.sh
	
								sed -i "s/APRICODD_ERROR/0.1/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/XION_DO/$Xion/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/STAT_V/false/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/MARKVISITED/true/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/MARKSOLVED/false/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/INITREWARD/true/" /tmp/crossing/$JOB_NAME.sh
								sed -i "s/REMEMBERMODE/NONE/" /tmp/crossing/$JOB_NAME.sh

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

#	for nTraj in 100
#	do
#		for generalize in false true
#		do
#			for generalization_type in value action         
#			do
#				for path_type in PATH           
#				do
#					for consistency_type in NONE
#					do
#						for Xion in true 
#						do
#							for lookahead in 8 16 32	
#							do
#
#								JOB_NAME=crossing_traffic_hrmax_$nobj\_$lookahead\_$nTraj\_$timeOut\_$generalize\_$generalization_type\_$path_type\_$consistency_type\_$Xion
#								cp run_srtdp.sh /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_inst_mdp__$nobj.rddl/"   /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/PRUNING_ON/false/"  /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/NUM_TRAJECTORIES/$nTraj/"   /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/TIMEOUT_MINS/-1/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/LOOKAHEAD_DEPTH/$lookahead/" /tmp/crossing/$JOB_NAME.sh
#
#								sed -i "s/ON_POLICY_DEPTH/100/" /tmp/crossing/$JOB_NAME.sh
#
#								sed -i "s/DO_APRICODD/true/" /tmp/crossing/$JOB_NAME.sh
#	
#								sed -i "s/APRICODD_ERROR/0.1/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/XION_DO/$Xion/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/STAT_V/false/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/GLOBAL_INIT/VMAX/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/LOCAL_INIT/HRMAX/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/TRUNCATE_TRIALS/false/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/MARKVISITED/true/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/MARKSOLVED/false/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/INITREWARD/true/" /tmp/crossing/$JOB_NAME.sh
#								sed -i "s/REMEMBERMODE/NONE/" /tmp/crossing/$JOB_NAME.sh
#
#								echo $JOB_NAME 
#						
#								if [ $generalize == false ]
#								then
#									break
#								fi	
#							done
#							
#							if [ $generalize == false ]
#							then
#								break
#							fi	
#
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

#	for numTraj in 100
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
#							cp run_srtdp.sh /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/NUM_STATE/$NUM_STATE/"  /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/"   /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_$nobj\_$nobj.rddl/"   /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/PRUNING_ON/false/"  /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/NUM_TRAJECTORIES/$numTraj/"   /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/TIMEOUT_MINS/-1/" /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_ON/$generalize/"  /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/GENERALIZATION_TYPE/$generalization_type/"   /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/PATH_TYPE/$path_type/" 	  /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/CONSISTENCY_TYPE/$consistency_type/"   /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/"  /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/LOOKAHEAD_DEPTH/8/" /tmp/crossing/$JOB_NAME.sh
#
#							sed -i "s/JOB_OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/crossing_log_v5\/$JOB_NAME.out/" /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/JOB_OUTPUT_ERROR/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/crossing_log_v5\/$JOB_NAME.err/" /tmp/crossing/$JOB_NAME.sh	
#							sed -i "s/OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/crossing_log_v5\/$JOB_NAME/" /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/ON_POLICY_DEPTH/8/" /tmp/crossing/$JOB_NAME.sh
#
#							sed -i "s/DO_APRICODD/true/" /tmp/crossing/$JOB_NAME.sh
#
#							sed -i "s/APRICODD_ERROR/0.1/" /tmp/crossing/$JOB_NAME.sh
#							sed -i "s/XION_DO/$Xion/" /tmp/crossing/$JOB_NAME.sh
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
