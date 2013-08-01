package rddl.competition.generators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DecoupledElevatorsGen {

	protected int nBuildings;
	
	protected final int outliersLow = 0;
	protected final int outliersHigh = 0;

	protected String output_dir;
	protected String instance_name;
	protected int els;
	protected int floors;
	protected float maxA;
	protected float minA;
	protected int hor;
	protected float dis;

	private int	curBuild;

	public static void main(String[] args) throws Exception {

		if (args.length != 9)
			usage();

		DecoupledElevatorsGen efg = new DecoupledElevatorsGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
		System.out.println(efg.output_dir+" "+efg.instance_name);
	}

	public static void usage() {
		System.err.println("Usage: output-dir instance-name numElevators numFloors max-arrive min_arrive horizon discount nbuildings");
		System.err.println("Example: files/testcomp/rddl elevator-1-4 1 4 0.3 0.1 100 0.9");
		System.exit(127);
	}

	public DecoupledElevatorsGen(String[] args) {
		// int els, int floors, int[]starts, float [] arrs, float pen, int
		// hor, float dis, int id, ){

		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		
		els = 0;
		floors = 0;
		try {
			els = Integer.parseInt(args[2]);
		} catch (Exception ex) {
			System.err.println("Number of elevators must be an integer");
			System.exit(127);
		}
		try {
			floors = Integer.parseInt(args[3]);
		} catch (Exception ex) {
			System.err.println("Number of floors must be an integer");
			System.exit(127);
		}

		maxA = Float.parseFloat(args[4]);
		minA = Float.parseFloat(args[5]);
		hor = Integer.parseInt(args[6]);
		dis = Float.parseFloat(args[7]);
		nBuildings = Integer.parseInt(args[8]);
		curBuild = 0;
	}

	public String generate() {
		++curBuild;
		
		Random ran = new Random();
		int[] starts = new int[els];
		for (int x = 0; x < els; x++)
			starts[x] = 0; //ran.nextInt(floors);

		List<Integer> lows = new ArrayList<Integer>();
		List<Integer> highs = new ArrayList<Integer>();
		while (lows.size() < outliersLow) {
			int r = ran.nextInt(floors);
			if (!lows.contains(r))
				lows.add(r);
		}

		while (highs.size() < outliersHigh) {
			int r = ran.nextInt(floors);
			if (!highs.contains(r) && !lows.contains(r))
				highs.add(r);
		}

		float[] arrs = new float[floors];
		try {
			for (int x = 0; x < floors; x++) {
				if (highs.contains(x))
					arrs[x] = 0.2f + ((ran.nextFloat() * (maxA - minA) + minA) / (floors));
				else if (lows.contains(x))
					arrs[x] = 0.0f;
				else
					arrs[x] = (ran.nextFloat() * (maxA - minA) + minA)
							/ (floors);
				if (arrs[x] > 1.0)
					arrs[x] = 1.0f;
			}
		} catch (Exception ex) {
			System.err.println("Arrival params must be floats");
			System.exit(127);
		}

		String s = "";
		s += "non-fluents nf_" + instance_name 
				+ " {\n\tdomain = elevators_buildings_mdp; \n\tobjects { \n";
		s += "\t\televator : {";
		for( int b = 0 ; b < nBuildings; ++b ) {
			for (int e = 0; e < els; e++) {
				s += "e" + b + "" + e;
				if ( !( e == els - 1 && b == nBuildings-1 ) ) {
					s += ",";
				}
			}
		}
		s += "};\n\t\tfloor : {";
		for( int b = 0 ; b < nBuildings; ++b ) {
			for (int e = 0; e < floors; e++) {
				s += "f" + b + "" + e;
				if ( ! ( e ==  floors - 1 && b  ==  nBuildings-1 ) ) {
					s += ",";
				}
			}
		}
		
		s += " }; \n";
		
		s += "\n\t\tbuilding : {";
		for( int b = 0 ; b < nBuildings; ++b ) {
			s += "b"+b;
			if( b != nBuildings-1 ) {
				s += ", ";
			}
		}
		s += " }; \n";
		
		s += "\t}; \n\tnon-fluents {\n";
		s += "\t\tELEVATOR-PENALTY-RIGHT-DIR = " + 0.75 + ";\n";
		s += "\t\tELEVATOR-PENALTY-WRONG-DIR = " + 3.00 + ";\n";
		
		for( int b = 0 ; b < nBuildings; ++b ) {
			for (int e = 0; e < floors; e++) {
				if (e != 0 && e != (floors-1))
					s += "\t\tARRIVE-PARAM(f" + b + "" + e + ") = " + arrs[e] + ";\n";
				if (e < floors - 1)
					s += "\t\tADJACENT-UP(f" + b + "" + e + ",f" + b + "" + (e + 1) + ") = true;\n";
			}	
		}
		
		for( int i = 0 ; i < nBuildings ; ++i ) {
			s += "\t\tTOP-FLOOR(f" + i + "" + (floors - 1) + ") = true;\n";
		}
		for( int i = 0 ; i < nBuildings ; ++i ) {
			for( int j = 0 ; j < floors ; ++j ) {
				s += "\t\tFLOOR-BUILDING(f"+i+""+j+",b"+i+");\n";	
			}
		}
		for( int i = 0 ; i < nBuildings ; ++i ) {
			for( int j = 0 ; j < els ; ++j ) {
				s += "\t\tELEVATOR-BUILDING(e"+i+""+j+",b"+i+");\n";	
			}
		}
		for( int i = 0 ; i < nBuildings ; ++i ) {
			s += "\t\tBOTTOM-FLOOR(f"+i+"0) = true;\n";	
		}
		s += "\t}; \n }\n";

		s += "instance " + instance_name + " { \n\tdomain = elevators_buildings_mdp; \n ";
		s += "\tnon-fluents = nf_" + instance_name + ";\n\tinit-state { \n";
		
		for( int i = 0 ; i < nBuildings ; ++i ) {
			for (int e = 0; e < els; e++) {
				s += "\t\televator-at-floor(e" +i + "" + e + ",f" + i +"" + starts[e] + ");\n";
			}
		}

		s += "\t};\n\tmax-nondef-actions = " + els + ";\n";
		s += "\thorizon = " + hor + ";\n";
		s += "\tdiscount = " + dis + ";\n} \n";

		return s;
	}

}
