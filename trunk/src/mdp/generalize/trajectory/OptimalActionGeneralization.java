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
		final ADDManager manager = parameters.get_manager();
		
		if( action == null ){
			return manager.getProductBDDFromAssignment( state.getFactoredState() );
		}
		
		final NavigableMap<String, Boolean> action_assign = action.getFactoredAction();
		final ADDRNode[] policies = parameters.get_policyDD();

		ADDRNode ret = null;// manager.DD_ZERO;
		
		final ADDRNode this_policy = policies[depth];
		final ADDRNode action_dd = manager.restrict(this_policy, action_assign);
		ret = generalize(action_dd, 
				parameters.get_genRule(), manager ,
				state.getFactoredState() );
		
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
