if [ $# != 3 ]
then
echo "Usage SeqHindsight.sh <rddl domain file> <rddl instance file> <epsilon> "
exit
fi

	
pkill -f "rddl.competition.Server"
rm /tmp/temprer -f

sh runserver.sh 100 > /tmp/temprer&
sleep 10s

java -Xms4g -Xmx4g -classpath bin/:lib/grappa1_4.jar:lib/java_cup.jar:lib/jlex.jar:lib/xercesImpl.jar:lib/xml-apis.jar:lib/hsqldb.jar mdp.solve.SeqHindsight $1 $2 $3