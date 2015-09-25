for nobj in 7 8 9                       
do
	for conc in 1 2 3 4 5  
	do 
		NUM_OBJECTS=$nobj

		for timeOut in 500           
		do
			for far in true false 
			do 
				for constr in true false
				do

					JOB_NAME=inventory_control_SPUDDFAR_$nobj\_$conc\_$timeOut\_far_$far\_naive_$constr 
					cp run_SPUDDFAR.sh /tmp/academic/$JOB_NAME.sh
					sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/inventory_control_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
					sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/inventory_control_inst_mdp_$nobj\_$conc.rddl/"   /tmp/academic/$JOB_NAME.sh
					sed -i "s/TIMEOUT_MINS/$timeOut/" /tmp/academic/$JOB_NAME.sh
					sed -i "s/JOB_NAME/$JOB_NAME/" /tmp/academic/$JOB_NAME.sh
		
					sed -i "s/USE_FAR/$far/" /tmp/academic/$JOB_NAME.sh

					sed -i "s/USE_DISCOUNTING/true/" /tmp/academic/$JOB_NAME.sh

					sed -i "s/USE_APRICODD/true/" /tmp/academic/$JOB_NAME.sh

					sed -i "s/APRICODD_EPSILON/0.1/" /tmp/academic/$JOB_NAME.sh

					sed -i "s/CONSTRAIN_NAIVELY/$constr/" /tmp/academic/$JOB_NAME.sh 


					echo $JOB_NAME 
				done
			done
		done
	done
done
