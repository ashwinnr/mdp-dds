package mdp.generalize.trajectory;

import java.util.NavigableMap;

import dd.DDManager.DDQuantify;

import add.ADDManager;
import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
import mdp.generalize.trajectory.parameters.OptimalActionParameters.UTYPE;
import mdp.generalize.trajectory.type.OptimalActionType;

public class OptimalActionGeneralization extends Generalization< 
		RDDLFactoredStateSpace, RDDLFactoredActionSpace,
		OptimalActionType, OptimalActionParameters > {

	@Override
	public ADDRNode generalize_state(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			FactoredState<RDDLFactoredStateSpace> next_state,
			OptimalActionParameters parameters, int depth) {
		//pick states where the action is the greedy action
		//does it have to be same depth?
		final boolean all_depth = parameters.get_Alldepth();
		final ADDManager manager = parameters.get_manager();
		
		if( depth == 0 || action == null ){
			return manager.getProductBDDFromAssignment( state.getFactoredState() );
			//so that init state is always same
		}
		
		final NavigableMap<String, Boolean> action_assign = action.getFactoredAction();
		final ADDRNode[] policies = parameters.get_policyDD();

		ADDRNode ret = null;// manager.DD_ZERO;
		
		if( all_depth ){
			
			for( int i = 0; i < policies.length; ++i ){
				final ADDRNode this_policy = policies[i];
				final ADDRNode action_dd = manager.restrict(this_policy, action_assign);
				final ADDRNode this_gen = generalize(action_dd, 
						 parameters.get_genRule(), 
						 manager,
						 state.getFactoredState() );
				
				//unionize!
				if( parameters.get_type().equals(UTYPE.DISJUNCT) ){
					ret = ret == null ? this_gen : manager.BDDUnion( ret, this_gen );
				}else if( parameters.get_type().equals(UTYPE.CONJUNCT) ){
					ret = ret == null ? this_gen : manager.BDDIntersection(ret, this_gen);
				}
			}
			
		}else{
			final ADDRNode this_policy = policies[depth];
			final ADDRNode action_dd = manager.restrict(this_policy, action_assign);
			ret = generalize(action_dd, 
					parameters.get_genRule(), manager ,
					state.getFactoredState() );
		}
		
		//factored action problem
		//ret may have action vars
		//since action can be partial
		//doing maximization
		ret = manager.quantify(ret, 
				parameters.get_actionVars(), DDQuantify.EXISTENTIAL );
		
		return ret;
		
	}
	

	@Override
	public ADDRNode generalize_action(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			FactoredState<RDDLFactoredStateSpace> next_state,
			OptimalActionParameters parameters, int depth) {

		return parameters.get_manager().DD_ONE;
	
	}

//	@Override
//	public ADDRNode generalize_next_state(
//			FactoredState<RDDLFactoredStateSpace> state,
//			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
//			FactoredState<RDDLFactoredStateSpace> next_state,
//			OptimalActionParameters parameters, int depth) {
//
//		return null;
//		
//	}

}
