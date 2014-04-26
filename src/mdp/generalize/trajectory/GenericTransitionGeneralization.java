package mdp.generalize.trajectory;

import mdp.generalize.trajectory.GenericTransitionGeneralization.Consistency;
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
    private Consistency[] _cons;
    public enum Consistency{
	WEAK_POLICY, WEAK_ACTION, VISITED, BACKWARDS_WEAK_POLICY, BACKWARDS_WEAK_ACTION,
	NONE, BACKWARDS_STRONG
    }
    
    public GenericTransitionGeneralization( final ADDDecisionTheoreticRegression dtr ,
	    final Consistency[] cons ) {
	_dtr = dtr;
	_cons = cons;
    }
    
    @Override
    public ADDRNode generalize_state(
	    final FactoredState<RDDLFactoredStateSpace> state,
	    final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
	    final FactoredState<RDDLFactoredStateSpace> next_state,
	    final GenericTransitionParameters<T, P, RDDLFactoredStateSpace, RDDLFactoredActionSpace> parameters,
	    final int depth) {
	
	
	final ADDManager manager = parameters.get_manager();
	if( depth == 0 || parameters.getFix_start_state() || parameters.getNum_states() == 1 ){
	    return manager.getProductBDDFromAssignment(state.getFactoredState());
	}
	
	final Generalization<RDDLFactoredStateSpace, RDDLFactoredActionSpace, T, P> generalizer = parameters.getGeneralizer();
	final ADDRNode ret = generalizer.generalize_state(state, action, next_state, parameters.getParameters(), depth);
	final ADDRNode state_node = manager.getProductBDDFromAssignment(state.getFactoredState());
	
	return parameters.getNum_states() == -1 ? ret : 
		manager.BDDUnion( 
				state_node, 
				manager.sampleBDD(ret, parameters.get_rand(), parameters.getNum_states()-1 ) );
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
	
	final ADDRNode ret = parameters.getGeneralizer().generalize_action(state, 
			action, next_state, parameters.getParameters(), depth);
	//pick only actions that are legal
	    
	return parameters.getNum_actions() == -1 ? ret : 
		parameters.get_manager().sampleBDD( ret, parameters.get_rand(), parameters.getNum_actions() );
    }
    
    public ADDRNode[] generalize_trajectory_forwards( 
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
	
	if( containsBackwards( _cons ) ){
		try {
			throw new Exception("Forward consistency with backwards rule");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	final ADDRNode[] ret = new ADDRNode[ states.length + actions.length ];
	final ADDManager manager = parameters.get_manager();
	
	ADDRNode prev_gen_state = null;
	ADDRNode prev_action = null;
	int j = 0;
	
	for( int i = 0; i < states.length && states[i].getFactoredState() != null ; ++i ){
//	    System.out.println("Generalizing state " + i );
	    
	    final ADDRNode cur_gen_state = generalize_state(states[i], 
	    		i >= actions.length ? null : actions[i], 
	    		i+1 >= states.length ? null : states[i+1], parameters, i);
	    
	    try{
	    	if( cur_gen_state.equals(manager.DD_ZERO) ){
	    		throw new Exception("WARNING generalized state is zero");
	    	}else if( 
	    			manager.restrict( cur_gen_state, states[i].getFactoredState() ).equals( 
	    					manager.DD_ZERO ) ){
	    		throw new Exception("WARNING generalized state does not contain state" );
	    	}
	    }catch( Exception e ){
	    	e.printStackTrace();
	    	System.exit(1);
	    }
	    
//	    System.out.println("Generalizing action " + i );
	    
	    final ADDRNode cur_gen_action = 
		i == states.length - 1 ? null : generalize_action( states[i], actions[i], states[i+1], parameters, i );
	    
	    if( prev_gen_state == null ){
		ret[j++] = cur_gen_state;
		prev_gen_state = cur_gen_state;
		
	    }else{
//		System.out.println("Consistency check" + i );
		ADDRNode consistent_cur_gen_state = cur_gen_state;
		
		for( final Consistency consistency : _cons ){
		    switch( consistency ){
		    case NONE : 
			break;
		    case WEAK_ACTION :
			consistent_cur_gen_state = 
//				parameters.get_constrain_naively() ?
						manager.BDDIntersection(consistent_cur_gen_state, 
				_dtr.BDDImageAction(prev_gen_state, DDQuantify.EXISTENTIAL, 
					prev_action, true , true)   ) ;
//					: 
//						manager.constrain(consistent_cur_gen_state, 
//								_dtr.BDDImagePolicy(prev_gen_state, true, DDQuantify.EXISTENTIAL, 
//									prev_action, true ), 
//									manager.DD_ZERO ) ;
			break;
		    case WEAK_POLICY : 
				final ADDRNode bddImagePolicy = _dtr.BDDImagePolicy(prev_gen_state, true, DDQuantify.EXISTENTIAL, 
					parameters.get_policyDD()[i-1], true);
				consistent_cur_gen_state = 
//				parameters.get_constrain_naively() ? 
						manager.BDDIntersection( consistent_cur_gen_state, bddImagePolicy  );
//					: manager.constrain(consistent_cur_gen_state, 
//							_dtr.BDDImagePolicy(prev_gen_state, true, DDQuantify.EXISTENTIAL, 
//									parameters.get_policyDD()[i-1], true) ,
//									manager.DD_ZERO );
			break;
//		   
		    case VISITED :
		    	
			consistent_cur_gen_state = 
//			    
//			parameters.get_constrain_naively()  ?
					manager.BDDIntersection( consistent_cur_gen_state, 
				parameters.get_visited()[i] );
//				: manager.constrain(consistent_cur_gen_state, parameters.get_visited()[i], 
//						manager.DD_ZERO );
			break;
//
		    case BACKWARDS_WEAK_ACTION:
			break;
		    case BACKWARDS_WEAK_POLICY:
			break;
			}
		}
		
		if( consistent_cur_gen_state.equals(manager.DD_ZERO) ){
		    try{
		    	throw new Exception("WARNING consistent generalized state is zero");
		    }catch( Exception e ){
		    	e.printStackTrace();
		    	System.exit(1);
		    }
		}
		
		ret[j++] = consistent_cur_gen_state;
		prev_gen_state = consistent_cur_gen_state;
	    }
	    
	    if( cur_gen_action != null ){
		ret[j++] = cur_gen_action;
	    }
	    
	    prev_action = i == states.length-1 || actions[i].getFactoredAction() == null 
	    		? null : manager.getProductBDDFromAssignment( actions[i].getFactoredAction() );
	}

	return ret;
    }

    @Override
    public ADDRNode[] generalize_trajectory(final FactoredState<RDDLFactoredStateSpace>[] states,
    		final FactoredAction<RDDLFactoredStateSpace,RDDLFactoredActionSpace>[] actions,
    		final GenericTransitionParameters<T,P,RDDLFactoredStateSpace,RDDLFactoredActionSpace> parameters) {
    	if( containsBackwards(_cons) ) {
    		return generalize_trajectory_backwards(states, actions, parameters);
    	}else{
    		return generalize_trajectory_forwards(states, actions, parameters);
    	}
    }
    
    private boolean containsBackwards(Consistency[] cons) {
    	for( final Consistency c : cons ){
    		if( c.equals(Consistency.BACKWARDS_WEAK_ACTION) || c.equals(Consistency.BACKWARDS_WEAK_POLICY) ){
    			return true;
    		}
    	}
    	return false;
	}

	public ADDRNode[] generalize_trajectory_backwards( 
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
	
	if( containsForwards( _cons ) ){
		try{
			throw new Exception("backward consistency with forward rule");
		}catch( Exception e ){
			e.printStackTrace();
			System.exit(1);
		}
	}
		
	final ADDRNode[] ret = new ADDRNode[ states.length + actions.length ];
	final ADDManager manager = parameters.get_manager();
	
	ADDRNode prev_gen_state = null;
	ADDRNode prev_action = null;
	int j = states.length + actions.length - 1; 
	
	for( int i = states.length-1; i >= 0; --i ){
//	    System.out.println("Generalizing state " + i );
	    if( states[i].getFactoredState() == null ) {
	    	continue;
	    }

	    final ADDRNode cur_gen_state = generalize_state(states[i], 
	    		i == 0 ? null : actions[i-1], 
	    		i == 0 ? null : states[i-1], parameters, i);
	    
	    if( cur_gen_state.equals(manager.DD_ZERO) ){
	    	try{
	    		throw new Exception("WARNING generalized state is zero");
	    	}catch(Exception e ){
	    		e.printStackTrace();
	    		System.exit(1);
	    	}
	    }
	    
//	    System.out.println("Generalizing action " + i );
	    
	    final ADDRNode cur_gen_action = 
	    		i == 0 ? null : generalize_action( states[i], actions[i-1], states[i-1], parameters, i-1 );
	    
	    if( prev_gen_state == null ){
	    	ret[j--] = cur_gen_state;
	    	prev_gen_state = cur_gen_state;
	    }else{
//		System.out.println("Consistency check" + i );
		ADDRNode consistent_cur_gen_state = cur_gen_state;
		for( final Consistency consistency : _cons ){
		    switch( consistency ){
		    case NONE :
			break;
		    case VISITED :
			consistent_cur_gen_state = manager.BDDIntersection( consistent_cur_gen_state, 
				parameters.get_visited()[i] );
			break;
		    
		    case BACKWARDS_WEAK_POLICY : 
		    	final ADDRNode preimage = _dtr.BDDPreImagePolicy( prev_gen_state, parameters.get_policyDD()[i],
		    		true, DDQuantify.EXISTENTIAL, false );
		    	consistent_cur_gen_state = manager.constrain(consistent_cur_gen_state, 
		    			preimage, manager.DD_ZERO );
		    	break;
		    case BACKWARDS_WEAK_ACTION : 
		    	final ADDRNode preimage_1 = _dtr.BDDPreImagePolicy(prev_gen_state,
		    			prev_action, true, DDQuantify.EXISTENTIAL, 
		    			false );
		    	consistent_cur_gen_state = manager.constrain(consistent_cur_gen_state, 
		    			preimage_1, manager.DD_ZERO );
		    	break;
		    case WEAK_ACTION:
			break;
		    case WEAK_POLICY:
			break;
		    }
		}
		
		if( consistent_cur_gen_state.equals(manager.DD_ZERO) ){
		    try{
		    	throw new Exception("WARNING consistent generalized state is zero");
		    }catch( Exception e ){
		    	e.printStackTrace();
		    	System.exit(1);
		    }
		}
		
		ret[j--] = consistent_cur_gen_state;
		prev_gen_state = consistent_cur_gen_state;
	    }
	    
	    if( cur_gen_action != null ){
		ret[j--] = cur_gen_action;
	    }
	    
	    prev_action = i == 0  
	    		? null : manager.getProductBDDFromAssignment( actions[i-1].getFactoredAction() );
	}

	return ret;
    }

	private boolean containsForwards(Consistency[] cons) {
		for( final Consistency c : cons ){
			if( c.equals(Consistency.WEAK_ACTION) || c.equals(Consistency.WEAK_POLICY) ){
				return true;
			}
		}
		return false;
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
