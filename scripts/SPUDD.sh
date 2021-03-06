if [ $# != 5 ]
then
echo "Usage SPUDD.sh <rddl domain file> <rddl instance file> <epsilon>  <num rounds> <num states>"
exit
fi

	
pkill -f "rddl.competition.Server"
rm -rf /tmp/temprer
sh runserver.sh $4 $5 > /tmp/temprer &

sleep 10s

java -Xms6g -Xmx6g -classpath bin/:lib/grappa1_4.jar:lib/java_cup.jar:lib/jlex.jar:lib/xercesImpl.jar:lib/xml-apis.jar:lib/hsqldb.jar mdp.solve.SPUDD $1 $2 $3 

cp /tmp/temprer ./tmp/temprer
