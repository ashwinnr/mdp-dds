for comp in 9 10 11 
do
	for conc in 1 2 3 4 5 6 7 
	do
		NUM_OBJECTS=$conc

		for timeOut in 500     
		do
			for bigdd in 1000 10000 100000  
			do
				JOB_NAME=sysadmin_uniring_MBFAR_$comp\_$conc\_$timeOut\_$bigdd 
				cp run_MBFAR.sh /tmp/skill/$JOB_NAME.sh
				sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/sysadmin_mdp.rddl/" /tmp/skill/$JOB_NAME.sh
				sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/sysadmin_uniring_$comp\_$conc.rddl/"   /tmp/skill/$JOB_NAME.sh
				sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/skill/$JOB_NAME.sh
				sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/skill/$JOB_NAME.sh
				sed -i "s/USE_DISCOUNTING/true/" /tmp/skill/$JOB_NAME.sh
				sed -i "s/USE_APRICODD/true/" /tmp/skill/$JOB_NAME.sh
				sed -i "s/APRICODD_EPSILON/0.1/" /tmp/skill/$JOB_NAME.sh
				sed -i "s/CONSTRAIN_NAIVELY/true/" /tmp/skill/$JOB_NAME.sh
				sed -i "s/BIG_LONG_DD/$bigdd/" /tmp/skill/$JOB_NAME.sh
				echo $JOB_NAME 
			done
		done
	done
done
