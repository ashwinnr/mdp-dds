for comp in 9 10 11 12 13 14 
do
	for conc in 8 9 10 11 12 13 14 
	do
		NUM_OBJECTS=$conc

		for timeOut in 500     
		do
			for steps in 2 5      
			do
				for constrain_naively in true false 
				do 
					JOB_NAME=sysadmin_star_MPI_$steps\_$comp\_$conc\_$timeOut\_constrain_naively_$constrain_naively
					cp run_MPI.sh /tmp/skill/$JOB_NAME.sh
					sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/sysadmin_mdp.rddl/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/sysadmin_star_$comp\_$conc.rddl/"   /tmp/skill/$JOB_NAME.sh
					sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/USE_FAR/true/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/USE_DISCOUNTING/true/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/USE_APRICODD/true/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/APRICODD_EPSILON/0.1/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/CONSTRAIN_NAIVELY/$constrain_naively/" /tmp/skill/$JOB_NAME.sh
					sed -i "s/STEPS_POLICY_EVALUATION/$steps/" /tmp/skill/$JOB_NAME.sh
					echo $JOB_NAME 
				done
			done
		done
	done
done
