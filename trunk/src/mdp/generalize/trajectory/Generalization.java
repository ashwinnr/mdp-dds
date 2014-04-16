package mdp.generalize.trajectory;

import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;

import dd.DDManager.DDOper;

import mdp.generalize.trajectory.parameters.GeneralizationParameters;
import mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH;
import mdp.generalize.trajectory.type.GeneralizationType;
import util.UnorderedPair;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDRNode;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;

public abstract class Generalization<S extends FactoredStateSpace, 
	A extends FactoredActionSpace<S>,
	T extends GeneralizationType, U extends GeneralizationParameters<T> > {
	
	public ADDRNode generalize( final ADDRNode input, 
			final GENERALIZE_PATH rule ,
			final ADDManager manager,
			final NavigableMap<String, Boolean>... paths ){
		switch( rule ){
		case ALL_PATHS :
			ADDRNode eval = input;
			for( final NavigableMap<String, Boolean> path : paths ){
				eval = manager.evaluate( eval, path );
			}
			
			ADDLeaf leaf = (ADDLeaf)eval.getNode();
			return manager.all_paths_to_leaf(input, leaf); 
		case NONE :
			return manager.getProductBDDFromAssignment( paths );
		}
		return null;
	}
	
	public ADDRNode generalize( final ADDRNode input, 
			final ADDRNode paths,//0 -infty BDD
			final GENERALIZE_PATH rule ,
			final ADDManager manager ){
		switch( rule ){
		case ALL_PATHS :
			ADDRNode ret = manager.DD_ZERO;
			ADDRNode good_paths = manager.apply( input, paths, DDOper.ARITH_PLUS );
			Set<NavigableMap<String, Boolean>> non_neginf_paths  
				= manager.enumeratePaths( good_paths, false, true, manager.DD_NEG_INF, true );
			for( final NavigableMap<String, Boolean> one_path : non_neginf_paths ){
				ret = manager.BDDUnion(ret, generalize( input, rule , manager, one_path ) );
			}
			return ret;
			
		case NONE :
			ret = manager.DD_ZERO;
			good_paths = manager.apply( input, paths, DDOper.ARITH_PLUS );
			non_neginf_paths  
				= manager.enumeratePaths( good_paths, false, true, manager.DD_NEG_INF, true );
			for( final NavigableMap<String, Boolean> one_path : non_neginf_paths ){
				ret = manager.BDDUnion(ret, manager.getProductBDDFromAssignment(one_path) );
			}
			return ret;
		}
		return null;
	}
	
	public abstract ADDRNode generalize_state( final FactoredState<S> state,
			final FactoredAction<S,A> action,
			final FactoredState<S> next_state,
			final U parameters,
			final int depth );

	public abstract ADDRNode generalize_action( final FactoredState<S> state,
			final FactoredAction<S,A> action,
			final FactoredState<S> next_state,
			final U parameters ,
			final int depth  );
//	public abstract ADDRNode generalize_next_state( final FactoredState<S> state,
//			final FactoredAction<S,A> action,
//			final FactoredState<S> next_state,
//			final U parameters ,
//			final int depth );
	
//	public ADDRNode[] generalize_transition( final FactoredState<S> state,
//			final FactoredAction<S,A> action,
//			final FactoredState<S> next_state,
//			final U parameters,
//			final int depth  ){
//		ADDRNode[] ret = {
//				generalize_state( state, action, next_state, parameters, depth ),
//				generalize_action( state, action, next_state, parameters, depth ),
//				generalize_next_state( state, action, next_state, parameters, depth )
//		};
//		return ret;
//	}
	
	public ADDRNode[] generalize_trajectory( 
			final FactoredState<S>[] states,
			final FactoredAction<S,A>[] actions,
			final U parameters ){
		if( states.length != actions.length + 1 ){
			try {
				throw new Exception("Bummer");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		final ADDRNode[] ret= new ADDRNode[ states.length + actions.length ]; 
		
		int j = 0;
		for( int i = 0 ; i < actions.length ; ++i ){
			ret[j++] = generalize_state(states[i], actions[i], states[i+1], parameters, i);
			ret[j++] = generalize_action(states[i], actions[i], states[i+1], parameters, i);
		}

		ret[j] = parameters.get_manager().getProductBDDFromAssignment( states[ states.length - 1 ].getFactoredState() );

		return ret;
	}
	
}
