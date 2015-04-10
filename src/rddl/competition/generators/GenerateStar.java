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

public class GenerateStar {
	public int startNumComps;
	public int endNumComps;
	public int stepSize;
	public double discount;
	public int horizon;
	public double rebootProb;
	public String instPrefix;
	public int maxDegreePerComp;
	public String destDir;
	public int degree;
	
	public GenerateStar(int startNumComps, int endNumComps, int stepSize, double discount, 
			int horizon, double rebootProb, int maxDeg, String instPrefix, 
			String destDir, int degree) throws FileNotFoundException {
		super();
		this.startNumComps = startNumComps;
		this.endNumComps = endNumComps;
		this.stepSize = stepSize;
		this.discount = discount;
		this.horizon = horizon;
		this.rebootProb = rebootProb;
		this.maxDegreePerComp = maxDeg;
		this.instPrefix = instPrefix;
		this.destDir = destDir;
		this.degree = degree;
		generate();
	}
	
	public void generate() throws FileNotFoundException{
		
		
		
		for( int numcomps = startNumComps ; numcomps <= endNumComps ; numcomps += stepSize ){
			
			for( int this_degree = 1; this_degree <= degree; ++this_degree ){

				StringBuilder sb = new StringBuilder();
				int startindex = 1;
				int endindex = numcomps;
				String instance_name = instPrefix+numcomps+"_"+this_degree;
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
				
				//hub connects max degree
				//recurse
				
				Queue<Integer> hubs = new LinkedList<Integer>();
				Map<Integer, Integer> connectedTo = new HashMap<Integer, Integer>();
				int hub = startindex;
				int curr = startindex+1;
				while( curr <= endindex ){
					if( connectedTo.get(hub) != null && connectedTo.get(hub) == maxDegreePerComp ){
						hub = hubs.remove();
					}
					int connnow = connectedTo.get(hub) == null ? 0 : connectedTo.get(hub);
					sb.append("CONNECTED(c"+hub+" ,c" + curr +");\n");
					hubs.add(curr);
					++curr;
					connectedTo.put(hub, connnow+1);
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
				sb.append("\tmax-nondef-actions = " + this_degree + ";\n");
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
		new GenerateStar( 7, 15, 1, 1.0d, 40, 0.0, 3, "sysadmin_star_", "./rddl/", 7);
	}
	
}
