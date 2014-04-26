package mdp.generalize.trajectory;

import java.util.NavigableMap;

import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression;

import add.ADDManager;
import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import mdp.generalize.trajectory.parameters.EBLParams;
import mdp.generalize.trajectory.type.EBL;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;

public class EBLGeneralization extends Generalization<RDDLFactoredStateSpace
, RDDLFactoredActionSpace, EBL, EBLParams>{

	@Override
	public ADDRNode generalize_state(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			FactoredState<RDDLFactoredStateSpace> next_state,
			EBLParams parameters, int depth) {
		final NavigableMap<String, Boolean> state_assign = state.getFactoredState();
		final ADDManager manager = parameters.get_manager();
		final ADDRNode state_bdd = manager.getProductBDDFromAssignment(state_assign);
		
		final ADDRNode[] policies = parameters.get_policyDD();
		final ADDRNode policy = policies[depth];
		if( depth == policies.length-1 ){
			return state_bdd;
		}
		final ADDDecisionTheoreticRegression dtr = parameters.get_dtr();

		ADDRNode image, image_not, preimage_image, preimage_image_not ;
		
		if( parameters.getOn_policy() ){
			image = dtr.BDDImagePolicy(state_bdd, true, 
					DDQuantify.EXISTENTIAL, parameters.get_policyDD()[depth],
					true );
			image_not = manager.BDDNegate(image);
			
			preimage_image = dtr.BDDPreImagePolicy(image, parameters.get_policyDD()[depth], 
					true, DDQuantify.EXISTENTIAL, true );
			preimage_image_not = dtr.BDDPreImagePolicy(image_not, parameters.get_policyDD()[depth]
					, true,
					DDQuantify.EXISTENTIAL, true );
		}else{
			image = dtr.BDDImage(state_bdd, true, 
					DDQuantify.EXISTENTIAL );
			image_not = manager.BDDNegate(image);
			
			preimage_image = dtr.BDDPreImage(image,  
					true, DDQuantify.EXISTENTIAL, true );
			preimage_image_not = dtr.BDDPreImage(image_not, true,
					DDQuantify.EXISTENTIAL, true );	
		}
		
		//preim_im => ~preim_im_not
		final ADDRNode gen_state = manager.BDDUnion(
									manager.BDDNegate(preimage_image),
									manager.BDDNegate(preimage_image_not) );
		
		if( gen_state.equals(manager.DD_ZERO) ){
			try{
				throw new Exception("zero state");
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		return gen_state;
	}

	@Override
	public ADDRNode generalize_action(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			FactoredState<RDDLFactoredStateSpace> next_state,
			EBLParams parameters, int depth) {
		return parameters.get_manager().DD_ONE;
	}

}
