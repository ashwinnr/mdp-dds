#$1 domain
#$2 instance
#$3 epsilon
#$4 numrounds
#$5 numstates
#$6 grep range
#$7 outputfile
echo $1 $2 $3 $4 $5
sh scripts/SPUDD.sh $1 $2 $3 $4 $5
perl scripts/extractState.pl $2 /tmp/temprer $5 $6 > $7
