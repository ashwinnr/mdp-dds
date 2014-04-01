package mdp.generalize.trajectory;

import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GenericTransitionParameters;
import mdp.generalize.trajectory.type.GeneralizationType;
import mdp.generalize.trajectory.type.GenericTransitionType;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;
import add.ADDManager;
import add.ADDRNode;
import dd.DDManager.DDQuantify;
import dtr.add.ADDDecisionTheoreticRegression;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;

//takes any generalizer
//calls it on states,a ctions and next states
//ensure consistency of trajectories from root state
//	generalized trajectory always starts at root state
//	only legal actions are allowed at states
// 	only reachable next states are allowed
//can fix number of samples from generalized state - or - action
// ASSUMES : backup is always model based and not from gen(s) <- gen(s')
// but rather gen(s) <- image( gen(s) )

public class GenericTransitionGeneralization<  T extends GeneralizationType, P extends GeneralizationParameters<T> > extends 
Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, 
GenericTransitionType<T>, GenericTransitionParameters<T,P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> >{

    protected ADDDecisionTheoreticRegression _dtr;
    
    public GenericTransitionGeneralization( ADDDecisionTheoreticRegression dtr) {
	_dtr = dtr;
    }
    
    @Override
    public ADDRNode generalize_state(
	    final FactoredState<RDDLFactoredStateSpace> state,
	    final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
	    final FactoredState<RDDLFactoredStateSpace> next_state,
	    final GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> parameters,
	    final int depth) {
	
	
	if( depth == 0 || parameters.getFix_start_state() || parameters.getNum_states() == 1 ){
	    return parameters.get_manager().getProductBDDFromAssignment(state.getFactoredState());
	}
	
	final Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P> generalizer = parameters.getGeneralizer();
	final ADDRNode ret = generalizer.generalize_state(state, action, next_state, parameters.getParameters(), depth);
	return parameters.get_manager().sampleBDD(ret, parameters.get_rand(), parameters.getNum_states() );
    }

    @Override
    public ADDRNode generalize_action(
	    final FactoredState<RDDLFactoredStateSpace> state,
	    final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
	    final FactoredState<RDDLFactoredStateSpace> next_state,
	    final GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> parameters,
	    final int depth) {
	if( parameters.getFix_action() || parameters.getNum_actions() == 1 ){
	    return parameters.get_manager().getProductBDDFromAssignment( action.getFactoredAction() );
	}
	
	final ADDRNode ret = parameters.getGeneralizer().generalize_action(state, action, next_state, parameters.getParameters(), depth);
	//pick only actions that are legal
	    
	return parameters.get_manager().sampleBDD( ret, parameters.get_rand(), parameters.getNum_actions() );
    }
    
    @Override
    public ADDRNode[] generalize_trajectory( 
    		final FactoredState<RDDLFactoredStateSpace>[] states, 
	    final FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace>[] actions, 
	    final GenericTransitionParameters<T,P,RDDLFactoredStateSpace,RDDLFactoredActionSpace> parameters) {
	if( states.length != actions.length + 1 ){
	    try{
		throw new Exception("Improper trajectory");
	    }catch( Exception e ){
		e.printStackTrace();
		System.exit(1);
	    }
	}
	
	final ADDRNode[] ret = new ADDRNode[ states.length + actions.length ];
	final ADDManager manager = parameters.get_manager();
	
	ADDRNode prev_gen_state = null;
	ADDRNode prev_gen_action = null;
	int j = 0;
	
	for( int i = 0; i < states.length; ++i ){
	    final ADDRNode cur_gen_state = generalize_state(states[i], 
	    		i == states.length - 1 ? null : actions[i], 
	    		i == states.length - 1 ? null : states[i+1], parameters, i);
	    final ADDRNode cur_gen_action = 
		i == states.length - 1 ? null : generalize_action( states[i], actions[i], states[i+1], parameters, i );
	    
	    if( prev_gen_state == null ){
		ret[j++] = cur_gen_state;
	    }else{
		//cur gen state must be a valid successor of prev gen state and prev gen action
		prev_gen_state = manager.BDDIntersection(cur_gen_state, 
				_dtr.BDDImagePolicy(prev_gen_state, true, DDQuantify.EXISTENTIAL, prev_gen_action, true) );
		ret[j++] = prev_gen_state;
	    }
	    
	    if( cur_gen_action != null ){
		ret[j++] = cur_gen_action;
	    }
	    
	    prev_gen_action = cur_gen_action;
	}

	return ret;
    }

//    @Override
//    public ADDRNode generalize_next_state(
//	    FactoredState<RDDLFactoredStateSpace> state,
//	    FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
//	    FactoredState<RDDLFactoredStateSpace> next_state,
//	    GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> parameters,
//	    int depth) {
//	if( depth == 0 ){
//	    throw new Exception("depth = 0 but next state ");
//	}
//	
//	if( parameters.getFix_next_state() || parameters.getNum_next_states() == 1 ){
//	    return parameters.get_manager().getProductBDDFromAssignment(next_state.getFactoredState());
//	}
//	
//	ADDRNode ret = parameters.getGeneralizer().generalize_next_state(state, action, next_state, parameters.getParameters(), depth);
//	final ADDManager manager = parameters.get_manager();
//	manager.BDDIntersection( ret, _dtr.BDDImage(reachable_states, withActionVars, action_quantification))
//	
//    }

}
