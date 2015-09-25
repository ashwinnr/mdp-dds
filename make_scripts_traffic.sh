NUM_STATE=1
NUM_ROUNDS=30
for nobj in 1 3
do
	NUM_OBJECTS=$nobj

	for numTraj in 100 500 1000
	do
		for generalize in true  false
		do
			for generalization_type in action  value reward
			do
				for path_type in PATH
				do
					for consistency_type in WEAK_ACTION WEAK_POLICY 
					do
						JOB_NAME=traffic_$nobj\_$numTraj\_$generalize\_$generalization_type\_$path_type\_$consistency_type
						cp run_srtdp.sh /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/traffic_mdp.rddl/p" /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/NUM_STATE/$NUM_STATE/p"  /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/NUM_ROUNDS/$NUM_ROUNDS/p"   /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/traffic_inst_mdp__$nobj.rddl/p"   /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/PRUNING_ON/false/p"  /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/NUM_TRAJECTORIES/$numTraj/p"   /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/GENERALIZATION_ON/$generalize/p"  /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/GENERALIZATION_TYPE/$generalization_type/p"   /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/PATH_TYPE/$path_type/p" 	  /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/CONSISTENCY_TYPE/$consistency_type/p"   /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/NUM_OBJECTS/$NUM_OBJECTS/p"  /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/JOB_NAME/$JOB_NAME/p" /tmp/traffic_v2/$JOB_NAME.sh

						sed -i "s/LOOKAHEAD_DEPTH/5/p" /tmp/traffic_v2/$JOB_NAME.sh

						sed -i "s/JOB_OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/traffic_log_v2\/$JOB_NAME.out/p" /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/JOB_OUTPUT_ERROR/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/traffic_log_v2\/$JOB_NAME.err/p" /tmp/traffic_v2/$JOB_NAME.sh	
						sed -i "s/OUTPUT_FILE/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/traffic_log_v2\/$JOB_NAME/p" /tmp/traffic_v2/$JOB_NAME.sh
						sed -i "s/ON_POLICY_DEPTH/5/p" /tmp/traffic_v2/$JOB_NAME.sh

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
		done
	done
done
