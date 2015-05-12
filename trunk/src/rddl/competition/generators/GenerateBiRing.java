package rddl.competition.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class GenerateBiRing {
	public int startNumComps;
	public int endNumComps;
	public int stepSize;
	public double discount;
	public int horizon;
	public double rebootProb;
	public String instPrefix;
	public String destDir;
	private int end_degree;
	
	public GenerateBiRing(int startNumComps, int endNumComps, int stepSize, 
			double discount, int horizon, double rebootProb, 
			String instPrefix, String destDir, int end_degree) throws FileNotFoundException {
		super();
		this.startNumComps = startNumComps;
		this.endNumComps = endNumComps;
		this.stepSize = stepSize;
		this.discount = discount;
		this.horizon = horizon;
		this.rebootProb = rebootProb;
		this.instPrefix = instPrefix;
		this.destDir = destDir;
		this.end_degree = end_degree;
		generate();
	}
	
	public void generate() throws FileNotFoundException{
		
		
		for( int deg = 1; deg <= end_degree ;++deg ){
			
			for( int numcomps = startNumComps ; numcomps <= endNumComps ; numcomps += stepSize ){
				
				
				StringBuilder sb = new StringBuilder();
				
				int startindex = 1;
				int endindex = numcomps;
				
				String instance_name = instPrefix+numcomps+"_"+deg;
				
				sb.append("non-fluents nf_" + instance_name + " {\n");
				sb.append("\tdomain = sysadmin_mdp;\n");
				sb.append("\tobjects {\n");
	
				sb.append("\t\tcomputer : {");
				
				for (int i = startindex; i <= endindex ; i++){
					sb.append(((i == startindex) ? "" : ",") + "c" + i);
				}
				
				sb.append("};\n");
			
				sb.append("\t};\n");
				
				sb.append("\tnon-fluents {\n");
			
				sb.append("\t\tREBOOT-PROB = " + rebootProb + ";\n");
				
				for (int i = startindex; i <= endindex; i++) {
					int connTo  = (i == endindex ? startindex : i+1);
					sb.append("\t\tCONNECTED(c" + i + ",c" + connTo + ");\n");
					sb.append("\t\tCONNECTED(c" + connTo + ",c" + i + ");\n");
				}	
				
				sb.append("\t};\n");
				sb.append("}\n\n");
				
				sb.append("instance " + instance_name + " {\n");
				sb.append("\tdomain = sysadmin_mdp;\n");
				sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
				sb.append("\tinit-state {\n");
			
			// 	Always start with all computers running
				for (int i = startindex; i <= endindex ; i++){
					sb.append("\t\trunning(c" + i + ");\n");
				}
					
				sb.append("\t};\n\n");
				sb.append("\tmax-nondef-actions = " + deg + ";\n");
				sb.append("\thorizon  = " + horizon + ";\n");
				sb.append("\tdiscount = " + discount + ";\n");
			
				sb.append("}");
			
				String content = sb.toString();
				PrintStream ps = new PrintStream(
					new FileOutputStream(destDir + instance_name + ".rddl"));
				ps.println(content);
				ps.close();
				System.out.println(destDir + instance_name + ".rddl");
			}
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		new GenerateBiRing(7, 15, 1, 0.9d, 40, 0.0, "sysadmin_biring_", 
				"./rddl/", 7 );
	}
	
}
