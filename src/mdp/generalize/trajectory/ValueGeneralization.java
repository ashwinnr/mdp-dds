package mdp.generalize.trajectory;

import java.util.NavigableMap;

import mdp.generalize.trajectory.parameters.ValueGeneralizationParameters;
import mdp.generalize.trajectory.type.ValueGeneralizationType;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import add.ADDManager;
import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

public class ValueGeneralization extends 
	Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, 
	ValueGeneralizationType, ValueGeneralizationParameters>{

	@Override
	public ADDRNode generalize_state(
			final FactoredState<RDDLFactoredStateSpace> state,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			final FactoredState<RDDLFactoredStateSpace> next_state,
			final ValueGeneralizationParameters parameters,
			final int depth ) {
		final NavigableMap<String, Boolean> state_assign = state.getFactoredState();
		final ADDManager manager = parameters.get_manager();
		
		if( depth == 0 ){
			return manager.getProductBDDFromAssignment(state_assign);
			//so that init state is always same
		}
		
		final ADDRNode ret = generalize( (parameters.get_valueDD())[ depth ] , 
				parameters.get_genRule(),
				parameters.get_manager(),
				state_assign, action == null ? null : action.getFactoredAction() );
		return ret;
				
	}

//	@Override
//	public ADDRNode[] generalize_transition(
//			FactoredState<RDDLFactoredStateSpace> state,
//			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
//			FactoredState<RDDLFactoredStateSpace> next_state,
//			ValueGeneralizationParameters parameters,
//			final int depth ) {
//		final ADDRNode this_vfn = parameters.getValue_fn()[ depth ];
//		final ADDRNode[] state_gen = {
//			generalize_state(this_vfn, state.getFactoredState(), parameters.get_genRule(), parameters.get_manager() ),
//			null, null };
//		return state_gen; 
//	}

	@Override
	public ADDRNode generalize_action(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			FactoredState<RDDLFactoredStateSpace> next_state,
			ValueGeneralizationParameters parameters, int depth) {
		return parameters.get_manager().DD_ONE;
	}

//	@Override
//	public ADDRNode generalize_next_state(
//			FactoredState<RDDLFactoredStateSpace> state,
//			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
//			FactoredState<RDDLFactoredStateSpace> next_state,
//			ValueGeneralizationParameters parameters, int depth) {
//		return null;
//	}
	
	
}
