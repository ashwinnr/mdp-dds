for nobj in 6 7               
do
	for conc in 1                      
	do 
		NUM_OBJECTS=$nobj

		for timeOut in 500           
		do
			for far in true false 
			do 
				for constr in true false
				do

					JOB_NAME=crossing_traffic_FAR_$nobj\_$conc\_$timeOut\_far_$far\_naive_$constr 
					cp run_SPUDDFAR.sh /tmp/academic/$JOB_NAME.sh
					sed -i "s/DOMAIN_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_mdp.rddl/" /tmp/academic/$JOB_NAME.sh
					sed -i "s/INSTANCE_FILENAME/\/nfs\/stak\/students\/n\/nadamuna\/mdp-dds\/rddl\/crossing_traffic_$nobj\_$nobj.rddl/"   /tmp/academic/$JOB_NAME.sh
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
