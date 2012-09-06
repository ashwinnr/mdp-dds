if [ $# != 6 ]
then
echo "Usage viCut.sh <rddl domain file> <rddl instance file> <epsilon> <big add> <num rounds> <num states>"
exit
fi
	
pkill -f "rddl.competition.Server"
sh runserver.sh $5 $6 > /tmp/temprer&
sleep 10s

java -Xms4g -Xmx4g -classpath bin/:lib/grappa1_4.jar:lib/java_cup.jar:lib/jlex.jar:lib/xercesImpl.jar:lib/xml-apis.jar:lib/hsqldb.jar mdp.solve.MBFAR $1 $2 $3 $4