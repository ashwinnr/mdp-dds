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

public class InventoryGenerator {
	
	public int startShops;
	public int endShops;
	public int stepSize;
	
	public double discount;
	public int horizon;
	
	public double minArrivalProb;
	public double maxArrivalProb;
	
	public double penalty;
	
	public int startTrucks;
	public int endTrucks;
	public int truckStepSize;
	
	public String instPrefix;
	public String destDir;
	
	
	
	/**
	 * @param pStartShops
	 * @param pEndShops
	 * @param pStepSize
	 * @param pDiscount
	 * @param pHorizon
	 * @param pMinArrivalProb
	 * @param pMaxArrivalProb
	 * @param pPenalty
	 * @param pStartTrucks
	 * @param pEndTrucks
	 * @param pTruckStepSize
	 * @param pInst
	 * @param pInstPrefix
	 * @param pDestDir
	 * @throws FileNotFoundException 
	 */
	public InventoryGenerator(int pStartShops, int pEndShops, int pStepSize,
			double pDiscount, int pHorizon, double pMinArrivalProb,
			double pMaxArrivalProb, double pPenalty, int pStartTrucks,
			int pEndTrucks, int pTruckStepSize,
			String pInstPrefix, String pDestDir) throws FileNotFoundException {
		super();
		this.startShops = pStartShops;
		this.endShops = pEndShops;
		this.stepSize = pStepSize;
		this.discount = pDiscount;
		this.horizon = pHorizon;
		this.minArrivalProb = pMinArrivalProb;
		this.maxArrivalProb = pMaxArrivalProb;
		this.penalty = pPenalty;
		this.startTrucks = pStartTrucks;
		this.endTrucks = pEndTrucks;
		this.truckStepSize = pTruckStepSize;
		this.instPrefix = pInstPrefix;
		this.destDir = pDestDir;
		generate();
	}

	public void generate() throws FileNotFoundException{
		
		for( int degree = startTrucks; degree <= endTrucks; ++degree ) {
			for( int numShops = startShops ; numShops <= endShops ; numShops += stepSize ){
				
				
				StringBuilder sb = new StringBuilder();
				
				int startindex = 1;
				int endindex = numShops;
				
				String instance_name = instPrefix+numShops+"_"+degree;
				
				sb.append("non-fluents nf_" + instance_name + " {\n");
				sb.append("\tdomain = inventory_control_mdp;\n");
				sb.append("\tobjects {\n");

				sb.append("\t\tshop : {");
				
				for (int i = startindex; i <= endindex ; i++){
					sb.append(((i == startindex) ? "" : ",") + "s" + i);
				}
				
				sb.append("};\n");
			
				sb.append("\t};\n");
				
				sb.append("\tnon-fluents {\n");
			
				for (int i = startindex; i <= endindex ; i++){
					double arr = Math.random()*(maxArrivalProb-minArrivalProb) + minArrivalProb;
					sb.append("\t\tARRIVE_PARAM(s" + i + ") = " + arr
							+ ";\n");
					sb.append("\t\tSTORE_PENALTY(s" + i + ") = " + penalty + ";\n");	
				}
				
				
				sb.append("\t};\n");
				sb.append("}\n\n");
				
				sb.append("instance " + instance_name + " {\n");
				sb.append("\tdomain = inventory_control_mdp;\n");
				sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
				sb.append("\tinit-state {\n");
			
			// 	Always start with all shops empty
				for (int i = startindex; i <= endindex ; i++){
					sb.append("\t\tempty(s" + i + ");\n");
				}
					
				sb.append("\t};\n\n");
				sb.append("\tmax-nondef-actions = " + degree + ";\n");
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
		new InventoryGenerator(4, 15, 1, 0.9d, 40, 0.05, 0.05, 0.35,
				1, 10, 1, "inventory_control_inst_mdp_", "./rddl/");
	}
	
}
