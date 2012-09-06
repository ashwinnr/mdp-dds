dom=./rddl/elevators_saket.rddl
numstates=15
numrounds=20
epsilon=0.1

for f in {2,3,4,5,6,7,8}
do
inst=./rddl/elevators_saket_$f.rddl
echo "Solving $dom $inst"
sh ./scripts/SPUDD.sh $dom $inst $epsilon $numrounds $numstates
echo "solved $f using VI"
mv ./tmp/temprer ./tmp/elevators_{$f}_add
sh ./scripts/SPUDDAADD.sh $dom $inst $epsilon $numrounds $numstates
echo "SPUDD aadd"
mv ./tmp/temprer ./tmp/elevators_{$f}_aadd
done
