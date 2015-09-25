for nobj in 1 2   
do
	NUM_OBJECTS=$nobj

	for timeOut in 500 
	do
		JOB_NAME=recon_FAR_$nobj\_$timeOut
		cp run_SPUDDFAR.sh /tmp/skill/$JOB_NAME.sh
		sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/recon_mdp.rddl/" /tmp/skill/$JOB_NAME.sh
		sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/recon_inst_mdp__$nobj.rddl/"   /tmp/skill/$JOB_NAME.sh
		sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/skill/$JOB_NAME.sh
		sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/skill/$JOB_NAME.sh
		echo $JOB_NAME 
	done
done
