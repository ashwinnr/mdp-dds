package rddl.competition.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class GenerateUniRing {

		protected String output_dir;
		protected String instance_prefix;
		protected int start_comp;
		protected float reboot_prob;
		protected int horizon;
		protected float discount;
		private final int MAX_COMPS = 25;
		private int end_degree;

		public static void main(String [] args) throws Exception {
			
			if(args.length != 7)
				usage();
			
			GenerateUniRing gen = new GenerateUniRing(args);
			
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name start-comp reboot-prob horizon discount end-degree");
		System.err.println("Example: files/testcomp/rddl sysadmin_10_3 10 0.05 100 0.9 7");
		System.exit(127);
	}
	
	public  GenerateUniRing(String [] args) throws FileNotFoundException{
		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_prefix = args[1];
		start_comp = Integer.parseInt(args[2]);
		reboot_prob = Float.parseFloat(args[3]);
		horizon = Integer.parseInt(args[4]);
		discount = Float.parseFloat(args[5]);
		end_degree = Integer.parseInt( args[6] );
		generate();
	}

	public void generate() throws FileNotFoundException{

		for( int deg = 1; deg <= end_degree; ++ deg ){
			int startindex = 101;
			
			for( int num_comp = start_comp ; num_comp <= MAX_COMPS ; num_comp += 1 ){
				StringBuilder sb = new StringBuilder();
				String instance_name = instance_prefix+"_"+num_comp+"_"+deg;
				
				sb.append("non-fluents nf_" + instance_name + " {\n");
				sb.append("\tdomain = sysadmin_mdp;\n");
				sb.append("\tobjects {\n");
		
				sb.append("\t\tcomputer : {");
				for (int i = startindex; i < startindex+num_comp; i++){
					sb.append(((i == startindex) ? "" : ",") + "c" + i);
				}
				sb.append("};\n");
				
				sb.append("\t};\n");
				
				sb.append("\tnon-fluents {\n");
				
				sb.append("\t\tREBOOT-PROB = " + reboot_prob + ";\n");
				
				for (int i = startindex; i < startindex+num_comp; i++) {
					sb.append("\t\tCONNECTED(c" + i + ",c" + (i == startindex+num_comp-1 ? startindex : i+1) + ");\n");
				}
				
				sb.append("\t};\n");
				sb.append("}\n\n");
				
				sb.append("instance " + instance_name + " {\n");
				sb.append("\tdomain = sysadmin_mdp;\n");
				sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
				sb.append("\tinit-state {\n");
				
				// Always start with all computers running
				for (int i = startindex; i < startindex+num_comp; i++)
					sb.append("\t\trunning(c" + i + ");\n");
				sb.append("\t};\n\n");
				sb.append("\tmax-nondef-actions = " + deg + ";\n");
				sb.append("\thorizon  = " + horizon + ";\n");
				sb.append("\tdiscount = " + discount + ";\n");
				
				sb.append("}");
				
				String content = sb.toString();
				PrintStream ps = new PrintStream(
						new FileOutputStream(output_dir + File.separator + instance_name + ".rddl"));
				ps.println(content);
				ps.close();
				System.out.println(output_dir + File.separator + instance_name + ".rddl");
			}
		}
	}
	
}
