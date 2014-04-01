package mdp.generalize.trajectory;

import java.util.NavigableMap;

import add.ADDManager;
import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import mdp.generalize.trajectory.parameters.OptimalActionParameters;
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
		final boolean all_depth = parameters.getAll_depth();
		final ADDManager manager = parameters.get_manager();
		
		if( depth == 0 ){
			return manager.getProductBDDFromAssignment( state.getFactoredState() );
			//so that init state is always same
		}
		
		final NavigableMap<String, Boolean> action_assign = action.getFactoredAction();
		final ADDRNode[] policies = parameters.getPolicies();

		ADDRNode ret = manager.DD_ZERO;
		
		if( all_depth ){
			
			//unionize!
			for( int i = 0; i < policies.length; ++i ){
				final ADDRNode this_policy = policies[i];
				final ADDRNode action_dd = manager.restrict(this_policy, action_assign);
				final ADDRNode this_gen = generalize(action_dd, state.getFactoredState(), parameters.get_genRule(), manager );
				ret = manager.BDDUnion( ret, this_gen );
			}
			
		}else{
			final ADDRNode this_policy = policies[depth];
			final ADDRNode action_dd = manager.restrict(this_policy, action_assign);
			ret = generalize(action_dd, state.getFactoredState(), parameters.get_genRule(), manager );
		}
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
