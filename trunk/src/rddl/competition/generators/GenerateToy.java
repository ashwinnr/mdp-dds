package rddl.competition.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.distribution.BinomialDistribution;

public class GenerateToy {
	
	public int startX;
	public int endX;
	public int stepSize;
	public double discount;
	public int horizon;
	public String instPrefix;
	public String destDir;
	public int end_degree;
	
	public GenerateToy(final int startX, final int endX, final int stepSize, 
			final double discount, 
			final int horizon, 
			final int end_degree, 
			final String instPrefix, 
			final String destDir ) throws FileNotFoundException {
		super();
		this.startX = startX;
		this.endX = endX;
		this.stepSize = stepSize;
		this.discount = discount;
		this.horizon = horizon;
		this.instPrefix = instPrefix;
		this.destDir = destDir;
		this.end_degree = end_degree;
		generate();
	}
	
	public void generate() throws FileNotFoundException{
		
		for( int numX = startX ; numX <= endX ; numX += stepSize ){
			for( int this_degree = 1; this_degree <= end_degree; ++this_degree ){

				StringBuilder sb = new StringBuilder();
				int startindex = 1;
				int endindex = numX;
				String instance_name = instPrefix+numX+"_"+this_degree;
				sb.append("non-fluents nf_" + instance_name + " {\n");
				sb.append("\tdomain = toy_mdp;\n");
				sb.append("\tobjects {\n");
				sb.append("\t\tX : {");
				for (int i = startindex; i <= endindex ; i++){
					sb.append(((i == startindex) ? "" : ",") + "x" + i);
				}
				sb.append("};\n");
				sb.append("\t};\n");
				sb.append("\tnon-fluents {\n");
				
				final BinomialDistribution flip_prob = new BinomialDistribution(endX, 0.65);
				final BinomialDistribution high_prob = new BinomialDistribution(endX, 0.45);
				final DecimalFormat df = new DecimalFormat("#.######");
				
				for (int i = startindex; i <= endindex ; i++){
					//FLIP prob generated from 
					// a Binomial( X, 0.35 )
					//
					//HOLD prob gen from Binomial( X, 0.65 )
					//
					final double flip = flip_prob.probability(i);
					final double high = high_prob.probability(i);
					
					sb.append("HIGH-PROB(X" + i + ") = " + df.format( high ) + ";\n" );
					sb.append("FLIP-PROB(X" + i + ") = " + df.format( flip ) + ";\n" );
					
				}
				sb.append("\t};\n");
				sb.append("}\n\n");
				
				sb.append("instance " + instance_name + " {\n");
				sb.append("\tdomain = toy_mdp;\n");
				sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
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
		new GenerateToy( 5, 15, 1, 0.9d, 40, 5, "toy_isnt_mdp_", "./rddl/");
	}
	
}
