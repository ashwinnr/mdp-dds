for nobj in 1 2 3 4 5 6 
do
	NUM_OBJECTS=$nobj

	for timeOut in 500  
	do
		JOB_NAME=crossing_FAR_$nobj\_$timeOut
		cp run_SPUDDFAR.sh /tmp/crossing_v5/$JOB_NAME.sh
		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/crossing_v5/$JOB_NAME.sh
		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_inst_mdp__$nobj.rddl/"   /tmp/crossing_v5/$JOB_NAME.sh
		sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/crossing_v5/$JOB_NAME.sh
		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/crossing_v5/$JOB_NAME.sh
		echo $JOB_NAME 
	done
done
