package rddl.competition.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class GenerateDecoupledUnirings {
	protected String output_dir;
	protected String instance_prefix;
	protected int start_comp;//per part
	protected float reboot_prob;
	protected int horizon;
	protected float discount;
	public int max_comps ;
	private int nparts;
	public int stepSize;
	public int nConnBetween;
	
	public GenerateDecoupledUnirings(String output_dir, String instance_prefix, int start_comp, float reboot_prob, int horizon, float discount, int nparts, int max_comp, int stepSize, int nConnBetw) throws Exception {
		super();
		this.output_dir = output_dir;
		this.instance_prefix = instance_prefix;
		this.start_comp = start_comp;
		this.reboot_prob = reboot_prob;
		this.horizon = horizon;
		this.discount = discount;
		this.nparts = nparts;
		this.stepSize = stepSize;
		this.max_comps = max_comp;
		this.nConnBetween = nConnBetw;		
		generate();
	}

	private void generate() throws Exception {
		
		for( int num_comp = start_comp ; num_comp <= max_comps ; num_comp += stepSize ){
			
//			for( int part = 0 ; part < nparts ; ++part){
//				int startindex = 1+(part*max_comps);
//				
//				StringBuilder sb_part = new StringBuilder();
//				
//				String instance_name = instance_prefix+"_"+nparts+"_"+num_comp+"_"+(part+1);
//				
//				sb_part.append("non-fluents nf_" + instance_name + " {\n");
//				sb_part.append("\tdomain = sysadmin_mdp;\n");
//				sb_part.append("\tobjects {\n");
//	
//				sb_part.append("\t\tcomputer : {");
//				
//				for (int i = startindex; i < startindex+num_comp; i++){
//					sb_part.append(((i == startindex) ? "" : ",") + "c" + i);
//				}
//				
//				sb_part.append("};\n");
//			
//				sb_part.append("\t};\n");
//				
//				sb_part.append("\tnon-fluents {\n");
//			
//				sb_part.append("\t\tREBOOT-PROB = " + reboot_prob + ";\n");
//			
//				for (int i = startindex; i < startindex+num_comp; i++) {
//					sb_part.append("\t\tCONNECTED(c" + i + ",c" + (i == startindex+num_comp-1 ? startindex : i+1) + ");\n");
//				}	
//			
//				sb_part.append("\t};\n");
//				sb_part.append("}\n\n");
//			
//				sb_part.append("instance " + instance_name + " {\n");
//				sb_part.append("\tdomain = sysadmin_mdp;\n");
//				sb_part.append("\tnon-fluents = nf_" + instance_name + ";\n");
//				sb_part.append("\tinit-state {\n");
//			
//			// 	Always start with all computers running
//				for (int i = startindex; i < startindex+num_comp; i++)
//					sb_part.append("\t\trunning(c" + i + ");\n");
//				sb_part.append("\t};\n\n");
//				sb_part.append("\tmax-nondef-actions = 1;\n");
//				sb_part.append("\thorizon  = " + horizon + ";\n");
//				sb_part.append("\tdiscount = " + discount + ";\n");
//			
//				sb_part.append("}");
//			
//				String content = sb_part.toString();
//				PrintStream ps = new PrintStream(
//					new FileOutputStream(output_dir + File.separator + instance_name + ".rddl"));
//				ps.println(content);
//				ps.close();
//			}
			
			//generate combined file
			StringBuilder sb_comb = new StringBuilder();
			String instance_name = instance_prefix+"_"+nparts+"_"+num_comp+"_"+nConnBetween;
			System.out.println(instance_name);
			
			sb_comb.append("non-fluents nf_" + instance_name + " {\n");
			sb_comb.append("\tdomain = sysadmin_mdp_same;\n");
			sb_comb.append("\tobjects {\n");

			sb_comb.append("\t\tcomputer : {");
			
			for( int part = 0 ; part < nparts ; ++part){
				int startindex = 1+(part*max_comps);
				int endindex = startindex+num_comp;
				
				for( int i = startindex ; i < endindex ; ++i ){
					sb_comb.append((( part == 0 && i == startindex ) ? "" : ",") + "c" + i);
				}
			}
				
			sb_comb.append("};\n");
		
			sb_comb.append("\t};\n");
				
			sb_comb.append("\tnon-fluents {\n");
			
			sb_comb.append("\t\tREBOOT-PROB = " + reboot_prob + ";\n");

			for( int part = 0 ; part < nparts ; ++part){
				int startindex = 1+(part*max_comps);
				int endindex = startindex+num_comp;
				
				for( int i = startindex ; i < endindex ; ++i ){
					sb_comb.append("\t\tCONNECTED(c" + i + ",c" + 
							(i == endindex-1 ? startindex : i+1) + ");\n");
				}
				
				for( int i = startindex ; i < endindex ; ++i ){
					for( int j = i+1; j < endindex; ++j ){
						sb_comb.append("\t\tSAME(c" + i + ", c" + j + ");\n" );	
					}
				}

				for( int i = 0 ; i < nConnBetween ; ++i ){
					if( startindex + i >= endindex ){
						System.err.println("nConn is too big as compared to nComps");
						System.exit(1);
					}
			
					int src = startindex+i;
					int dest = i+1+((part+1)%nparts)*max_comps;
					
					sb_comb.append("\t\tCONNECTED(c"+src+",c"+dest+");\n");
				}				
			}

			
			sb_comb.append("\t};\n");
			sb_comb.append("}\n\n");
			
			sb_comb.append("instance " + instance_name + " {\n");
			sb_comb.append("\tdomain = sysadmin_mdp_same;\n");
			sb_comb.append("\tnon-fluents = nf_" + instance_name + ";\n");
			sb_comb.append("\tinit-state {\n");
			
			// 	Always start with all computers running
			for( int part = 0 ; part < nparts ; ++part){
				int startindex = 1+(part*max_comps);
				int endindex = startindex+num_comp;
				
				for( int i = startindex ; i < endindex ; ++i ){
					sb_comb.append("\t\trunning(c" + i + ");\n");
				}
			}
			
			sb_comb.append("\t};\n\n");
			sb_comb.append("\tmax-nondef-actions = "+ nparts + ";\n");
			sb_comb.append("\thorizon  = " + horizon + ";\n");
			sb_comb.append("\tdiscount = " + discount + ";\n");
			
			sb_comb.append("}");
			
			String content = sb_comb.toString();
			PrintStream ps = new PrintStream(
				new FileOutputStream(output_dir + instance_name + ".rddl"));
			ps.println(content);
			ps.close();
			System.out.println(output_dir + instance_name + ".rddl");
			
		}
	}
	
	public static void main(String[] args) throws Exception {
		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 3, (float)0.05, 40, (float)0.9, 1, 3, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 6, (float)0.05, 40, (float)0.9, 2, 6, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 3, (float)0.05, 40, (float)0.9, 4, 3, 1,2);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 3, (float)0.05, 40, (float)0.9, 4, 3, 1,3);
		
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 6, (float)0.05, 40, (float)0.9, 3, 2, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 6, (float)0.05, 40, (float)0.9, 4, 2, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 6, (float)0.05, 40, (float)0.9, 5, 2, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 6, (float)0.05, 40, (float)0.9, 6, 2, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 2, (float)0.05, 40, (float)0.9, 7, 2, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 2, (float)0.05, 40, (float)0.9, 8, 2, 1,0);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 2, (float)0.05, 40, (float)0.9, 9, 2, 1,0);
//		
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 2, (float)0.05, 40, (float)1.0, 5, 2, 1,1);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 2, (float)0.05, 40, (float)1.0, 5, 2, 1,1);
//		new GenerateDecoupledUnirings("./rddl/", "sysadmin_ringofring", 2, (float)0.05, 40, (float)1.0, 7, 2, 1,0);
	}
}
