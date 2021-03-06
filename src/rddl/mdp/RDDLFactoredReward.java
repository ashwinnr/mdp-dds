package rddl.mdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import add.ADDManager;
import add.ADDRNode;

import com.google.common.base.Optional;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.PVAR_NAME;
import util.UnorderedPair;
import mdp.define.Action;
import mdp.define.State;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredReward;
import factored.mdp.define.FactoredState;

public class RDDLFactoredReward implements FactoredReward< RDDLFactoredStateSpace, RDDLFactoredActionSpace>{

	private rddl.State _state;
	private TreeMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> _rddlVars;
	private final static HashMap<LVAR, LCONST> empty_sub = new HashMap<LVAR, LCONST>();

	public RDDLFactoredReward(rddl.State rddlState, 
			NavigableMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > groundRDDLStateVars,
			final long seed, 
			NavigableMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> groundRDDLActionVars ) {
		_state = rddlState;
		_rddlVars = new TreeMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST>> >( groundRDDLStateVars );
		_rddlVars.putAll( groundRDDLActionVars );
	}
	
	@Override 
	public <T extends mdp.define.State<RDDLFactoredStateSpace>, 
	U extends mdp.define.Action<RDDLFactoredStateSpace,
	RDDLFactoredActionSpace>> double sample(T state, U action, Random rand) {
		return this.sample(state, action, rand, Optional.<List<ADDRNode>>absent(),
				Optional.<ADDManager>absent() );
		
	};
	
	public <T extends State<RDDLFactoredStateSpace>, U extends Action<RDDLFactoredStateSpace, 
		RDDLFactoredActionSpace>> double sample(
			T state, U action, 
			final Random random,
			final Optional<List<ADDRNode>> sum_of_reward_dds ,
			final Optional<ADDManager> dd_man ) {
		
		return sample( ((FactoredState<RDDLFactoredStateSpace>)state).getFactoredState(), 
				((FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>) action).getFactoredAction(),
				random );
				
	}
	
	private void setStateAction(
			final NavigableMap<String, Boolean> state_assign, 
			final NavigableMap<String, Boolean> action_assign ){

		for( Map.Entry<String, Boolean> entry : state_assign.entrySet() ){
			String varName = entry.getKey();
			UnorderedPair<PVAR_NAME, ArrayList<LCONST>> rddlPair 
				= _rddlVars.get( varName );
			_state.setPVariableAssign(rddlPair._o1, rddlPair._o2, entry.getValue() );
		}
		
		for( Map.Entry<String, Boolean> entry : action_assign.entrySet() ){
			String varName = entry.getKey();
			UnorderedPair<PVAR_NAME, ArrayList<LCONST>> rddlPair 
			= _rddlVars.get( varName );
			_state.setPVariableAssign(rddlPair._o1, rddlPair._o2, entry.getValue() );
		}
	}

	private void setStateAction(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action) {
		setStateAction( state.getFactoredState(), 
				action == null ? null : action.getFactoredAction() );
	}
	//this class must build
	//the propositional substitutions /
	//and sample from it
	//code replicated from RDDLTransition

	public double sample(final NavigableMap<String, Boolean> state_assign,
			final NavigableMap<String, Boolean> action_assign, final Random random ) {
		setStateAction( state_assign, action_assign  );
		
		try{
			_state.checkStateActionConstraints(null, false);
		}catch( EvalException e ){
			return Double.NEGATIVE_INFINITY;
		}
		
		try {
			return Double.valueOf( _state._reward.sample(empty_sub , _state, random).toString() );
		} catch (EvalException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return Double.NEGATIVE_INFINITY;

	}
	
}
