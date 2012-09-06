#!/usr/local/bin/perl

$instance_file = $ARGV[0];
$states_file = $ARGV[1];
$num_states = $ARGV[2];
$grep_range = $ARGV[3];


$problem_line = "p1";
@problems = ("p1");
for($i=2;$i<=$num_states; $i++) {
    push(@problems, "p$i"),
    $problem_line = $problem_line . ",p$i";
}
print "problems([$problem_line]).\n\n\n";


open(IN, "$instance_file");
$start = 0;
$domain = "[objects(maxobj,[a,a1])";
$nonfluents = "";
$horizon = 0;

while($line = <IN>) {
    if($line =~ /horizon = ([0-9]*)/) {
	$horizon = $1;
    }

    if($line =~ /non-fluents \{/) {
	$line = <IN>;
	$line =~ s/\s//g;
	$line =~ s/;/,/g;	
	$nonfluents = $nonfluents . lc($line);
    }

    if($line =~ /objects/) {
	while(($line = <IN>) =~ /:/) {
	    $line =~ s/\s//g;
	    $line =~ /(.*):\{(.*)\}/;
	    $domain = $domain . ",objects(" . $1 . ",[" . $2 . "])";
	}
    }
}


$domain = $domain . "]";
close(IN);


@lines = `cat $states_file | grep -A $grep_range "Round 1 / "`;

#print "@lines\n\n";

chomp(@lines);
$i = 0;
$prob = 0;
while($i <= $#lines) {
    if($lines[$i] =~ /states/) {	
	$curr_prob = $problems[$prob];
	$prob++;
	$newstate = "[" . $nonfluents;
	while($lines[$i] =~ /states/) {
	    if($lines[$i] =~ /- states: (.*) := true/) {
		$lit = $1;
		$lit =~ tr/[]/\(\)/;
		$newstate = $newstate . $lit . ",";
	    }
	    $i++;
	}
	$newstate = $newstate . "]";
	$newstate =~ s/,]/]/g;
	$newstate =~ s/ //g;
	print "testDomain($curr_prob, $domain).\n\n";
	print "initState($curr_prob, $newstate).\n\n";
	print "horizon($curr_prob,$horizon).\n\n\n\n";
    } else {
	$i++;
    }
}


