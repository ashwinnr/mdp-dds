/**
 * 
 */
package rddl.competition.generators;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author NR
 *
 * TODO
 */
public class JobShopGenerator {
	
	private int startJobs;
	private int endJobs;
	private int stepJobs;
	
	private int startMachines;
	private int endMachines;
	private int stepMachines;
	
	private int startDegree;
	private int endDegree;
	private int stepDegree;
	
	private double successParam;
	
	private String instPrefix;
	
	private String destDir;
	
	private Random _rand = new Random();
	
	private double horizon;
	private double discount;
	/**
	 * @param pStartJobs
	 * @param pEndJobs
	 * @param pStepJobs
	 * @param pStartMachines
	 * @param pEndMachines
	 * @param pStepMachines
	 * @param pStartDegree
	 * @param pEndDegree
	 * @param pStepDegree
	 * @param pSuccessParam
	 * @throws FileNotFoundException 
	 */
	public JobShopGenerator(int pStartJobs, int pEndJobs, int pStepJobs,
			int pStartMachines, int pEndMachines, int pStepMachines,
			int pStartDegree, int pEndDegree, int pStepDegree,
			double pSuccessParam, String instPrefix, String destDir,
			double discount, double horizon) throws FileNotFoundException {
		super();
		this.startJobs = pStartJobs;
		this.endJobs = pEndJobs;
		this.stepJobs = pStepJobs;
		this.startMachines = pStartMachines;
		this.endMachines = pEndMachines;
		this.stepMachines = pStepMachines;
		this.startDegree = pStartDegree;
		this.endDegree = pEndDegree;
		this.stepDegree = pStepDegree;
		this.successParam = pSuccessParam;
		this.instPrefix = instPrefix;
		this.destDir = destDir;
		this.horizon = horizon;
		this.discount = discount;
		generate();
	}

	/**
	 * @throws FileNotFoundException 
	 * 
	 */
	private void generate() throws FileNotFoundException {
		for( int degree = startDegree; degree <= endDegree; ++stepDegree ) {
			for( int jobs = startJobs; jobs <= endJobs; ++stepJobs ) {
				for( int machines = startMachines; machines <= endMachines; ++stepMachines ) {
					
					StringBuilder sb = new StringBuilder();
					String instance_name = instPrefix+"_"+jobs+"_"+machines+"_"+degree;
					
					sb.append("non-fluents nf_" + instance_name + " {\n");
					sb.append("\tdomain = job_shop_mdp;\n");
					sb.append("\tobjects {\n");
					sb.append("\t\tjob : {");
					
					for( int i = 1; i <= jobs; ++i ) {
						sb.append("j"+i+(i == jobs?" };\n":", "));
					}
					
					sb.append("\t\tmachine : {");
					for( int i = 1; i <= machines; ++i ) {
						sb.append("m"+i+(i==machines?" };\n":", "));
					}
					
					sb.append("\n\t};\n");
					
					sb.append("\tnon-fluents{\n");
					
					for( int i = 1; i <= jobs; ++i ) {
						sb.append("\t\tSUCCESS-PARAM(j"+i+") = " + successParam + ";\n");
					}
					
					for( int i = 1; i <= jobs; ++i ) {
						Set<Integer> pres = new HashSet<Integer>();
						while( pres.size() != degree ) {
							int rand = _rand.nextInt(machines)+1;
							pres.add(rand);
						}
						
						for( Integer pre : pres ) {
							sb.append("\t\tNEEDS(j"+i+", m"+pre+");\n");
						}
					}
					
					sb.append("\t};\n}\n");
					
					sb.append("instance "+ instance_name + "{\n");
					sb.append("\tdomain = job_shop_mdp;\n");
					sb.append("\tnon-fluents = nf_"+instance_name+";\n");
					sb.append("init-state{\n");
					
					for( int i = 1; i <= jobs; ++i ) {
						sb.append("\t\t~done(j"+i+");\n");
					}
					sb.append("\t};\n");
					
					sb.append("\tmax-nondef-actions = "+machines+";\n");
					sb.append("\thorizon = " + horizon+";\n");
					sb.append("\tdiscount = " + discount +";\n");
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
	}

	public static void main(String[] args) throws FileNotFoundException {
		new JobShopGenerator(6, 6, 1, 4, 4, 1, 3, 3, 1, 0.9, "job_shop_inst", "./rddl/", 
				0.9, 40);
	}
}
