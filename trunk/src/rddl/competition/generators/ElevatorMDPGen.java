package rddl.competition.generators;

/**
 *  A generator for instances of a fully observable elevators domain.
 *  
 *  @author Tom Walsh
 *  @version 2/18/11
 * 
 **/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.UniformRealDistribution;

import rddl.RDDL.Uniform;

public class ElevatorMDPGen {

	// parameters are number of elevators, number of floors, starting floor of
	// each elevator,
	// arrival parameter on each floor, horizon and discount

	public static void main(String[] args) throws Exception {
		if (args.length != 11)
			usage();
		ElevatorMDPGen efg = new ElevatorMDPGen(args);
//		String content = efg.generate();
//		PrintStream ps = new PrintStream(
//				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
//		ps.println(content);
//		ps.close();
//		System.out.println(efg.output_dir + File.separator + efg.instance_name + ".rddl");
	}

	public static void usage() {
		System.err.println("Usage: output-dir instance-prefix begin-floors end-floors " +
				"step-floors begin-elevs end-elevs min-arrive max_arrive horizon discount");
		System.exit(127);
	}

	public ElevatorMDPGen(String[] args) throws FileNotFoundException {
		// int els, int floors, int[]starts, float [] arrs, float pen, int
		// hor, float dis, int id){

		final String output_dir = args[0];
		final String inst_prefix = args[1];
		final int start_floors = Integer.parseInt( args[2] );
		final int end_floors = Integer.parseInt( args[3] );
		final int step_floors = Integer.parseInt( args[4] );
		final int begin_elevs = Integer.parseInt( args[5] );
		final int end_elevs = Integer.parseInt( args[6] );
		final double min_arrive = Double.parseDouble( args[7] );
		final double max_arrive = Double.parseDouble( args[8] );
		final int horizon = Integer.parseInt( args[9] );
		final double discount = Double.parseDouble( args[10] );
		final UniformRealDistribution rand = ( min_arrive == max_arrive ) ? 
				null : new UniformRealDistribution(min_arrive, max_arrive);
		
		for( int elev = begin_elevs; elev <= end_elevs; ++elev ){
			for( int floor = start_floors; floor <= end_floors; ++floor ){
				final String instance_name = inst_prefix + "_" + floor + "_" + elev;
				final StringBuffer sb_non_fluents = new StringBuffer();
				sb_non_fluents.append("non-fluents nf_" + instance_name + "{\n" );
				sb_non_fluents.append("\tdomain = elevators_mdp;\n");
				sb_non_fluents.append("\tobjects {\n");
				sb_non_fluents.append("\t\televator : {");
				for( int i = 0; i < elev; ++i ){
					sb_non_fluents.append("e"+i + (( i == elev-1 )? "};\n":",") );
				}
				sb_non_fluents.append("\t\tfloor : {");
				for( int i = 0 ; i < floor; ++i ){
					sb_non_fluents.append("f" + i + (( i == floor - 1 ) ? "};\n" : "," ));
				}
				sb_non_fluents.append("\t};\n");
				sb_non_fluents.append("\n");
				sb_non_fluents.append("\tnon-fluents {\n");
				for( int i = 0; i < floor; ++i ){
					final double arriv = ( min_arrive == max_arrive ) ? min_arrive : rand.sample();
					sb_non_fluents.append("\t\tARRIVE-PARAM(f" + i + ") = " + arriv +";\n");
				}
				sb_non_fluents.append("\t\tTOP-FLOOR(f" + (floor-1) + ") = true;\n");
				sb_non_fluents.append("\t\tBOTTOM-FLOOR(f" + 0 + ") = true;\n");
				for( int i = 0 ; i < floor-1; ++i ){
					sb_non_fluents.append("\t\tABOVE(f" + (i+1) + ", f" + i + ") = true;\n");
				}
				
				for( int i = 0; i < elev; ++i ){
					for( int j = i+1; j < elev; ++j ){
						sb_non_fluents.append("\t\tBEFORE(e" + i + ", e" + j + ") = true;\n");
					}
				}
				sb_non_fluents.append("\t};\n");
				sb_non_fluents.append("}\n");
				
				final StringBuffer sb_inst = new StringBuffer();
				sb_inst.append("instance " + instance_name + "{\n");
				sb_inst.append("\tdomain = elevators_mdp;\n");
				sb_inst.append("\tnon-fluents = nf_" + instance_name +";\n");
				sb_inst.append("\tmax-nondef-actions = " + elev + ";\n");
				sb_inst.append("\thorizon = " + horizon + ";\n" );
				sb_inst.append("\tdiscount = " + discount + ";\n" );
				sb_inst.append("}");
				
				final String all_of_it = sb_non_fluents.toString() + "\n" + sb_inst.toString();
				final String file_name = output_dir + instance_name + ".rddl";
				final PrintStream ps = new PrintStream(  new File( file_name ) );
				ps.println( all_of_it );
				ps.close();
				System.out.println( file_name );
			}
		}
	}
}
