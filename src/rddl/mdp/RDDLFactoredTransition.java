package rddl.mdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Maps;

import add.ADDManager;
import add.ADDRNode;

import mdp.define.Action;
import mdp.define.State;
import rddl.EvalException;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.Bernoulli;
import rddl.RDDL.CPF_DEF;
import rddl.RDDL.DiracDelta;
import rddl.RDDL.EXPR;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.KronDelta;
import rddl.RDDL.LCONST;
import rddl.RDDL.LTERM;
import rddl.RDDL.LVAR;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.viz.StateViz;
import util.UnorderedPair;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredTransition;

public class RDDLFactoredTransition extends RDDLConstrainedMDP implements 
	FactoredTransition< RDDLFactoredStateSpace, RDDLFactoredActionSpace> {

	private static final boolean DISPLAY_UPDATES = false;
	private rddl.State _state;
	private RDDLFactoredStateSpace _stateSpace;
	private RDDLFactoredActionSpace _actionSpace;
	private String[] _nextStateVars;
	private TreeMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> _rddlVars;
	private HashMap<LVAR, LCONST>[] _groundSubs;
	private EXPR[] _groundExpr;
	private String[] _stateVars;
	private RDDLConstrainedMDP _constraint;
	private NavigableMap< String, Boolean > _nextStateMap = new TreeMap<String, Boolean>();
	
	private static NavigableMap<String, Boolean> _defVal = Maps.newTreeMap();
	
	public RDDLFactoredTransition( rddl.State rddlState, 
			RDDLFactoredStateSpace rddlStateSpace,
			RDDLFactoredActionSpace rddlActionSpace,
			NavigableMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > groundRDDLStateVars,
			NavigableMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>> groundRDDLActionVars ) 
					throws EvalException{
		_state = rddlState;
		_stateSpace = rddlStateSpace;
		_actionSpace = rddlActionSpace;
		Set<String> stateVars = _stateSpace.getStateVariables();
		int size = stateVars.size();
		_nextStateVars = new String[ size ];
		_rddlVars = new TreeMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST>>>
			( groundRDDLStateVars );
		_rddlVars.putAll( groundRDDLActionVars );
		_groundSubs = new HashMap[ size ];
		_groundExpr = new EXPR[ size ];
		_stateVars = new String[ size ];
		//build everything here
		//sampleFactored() has to be light weight
		int i = 0;
		for( String s : stateVars ){
			_stateVars[i] = s.intern();
			String primedString = (s+"'").intern();
			_nextStateVars[i] = ( primedString );
			UnorderedPair<PVAR_NAME, ArrayList<LCONST>> rddlPair 
				= _rddlVars.get( s );
			PVAR_NAME primedPvar = new PVAR_NAME( (rddlPair._o1._sPVarName + "'").intern() );
			CPF_DEF cpf = _state._hmCPFs.get( primedPvar );
			EXPR cpf_expr = cpf._exprEquals;
			ArrayList<LTERM> termNames = cpf._exprVarName._alTerms;
			HashMap<LVAR, LCONST> subs = new HashMap<LVAR, LCONST>();
			
			for( int j = 0 ; j < termNames.size(); ++j ){
				subs.put( (LVAR)(termNames.get(j)), rddlPair._o2.get(j) );
			}
			_groundSubs[i] = subs;
			_groundExpr[i] = cpf_expr;
			_defVal.put( _stateVars[i], (Boolean) _state.getDefaultValue(rddlPair._o1) );
			++i;
		}
		_constraint = new RDDLConstrainedMDP();
	}
	
	@Override
	public <T extends State<RDDLFactoredStateSpace>,
			U extends Action<RDDLFactoredStateSpace, RDDLFactoredActionSpace> >
				T sample(
			final T state,
			final U action,
			final Random rand ) {
		return (T) sampleFactored( (FactoredState<RDDLFactoredStateSpace>)state, 
			(FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>) action, rand );
	}

	@Override
	public FactoredState<RDDLFactoredStateSpace> sampleFactored(
			final FactoredState<RDDLFactoredStateSpace> state,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action,
			final Random rand ) {
		
		try{
			setStateAction( state, action );
			_constraint.setState( _state );
			
			try {
				_constraint.checkConstraints( false );
			} catch (Exception e1) {
				e1.printStackTrace();
				System.out.println( "State : " + state.getFactoredState() );
				System.out.println( "Action : " + action.getFactoredAction() );
				System.exit(1);
			}
			
			for( int i = 0 ; i < _nextStateVars.length; ++i ){
				String thisOne = _nextStateVars[i];
				HashMap<LVAR, LCONST> subs = _groundSubs[i];
				EXPR expr = _groundExpr[i];
				EXPR e = expr.getDist(subs, _state);
				double prob_true = -1d;
				//System.out.println("RDDL.EXPR: " + e);
				if (e instanceof KronDelta) {
					EXPR e2 = ((KronDelta)e)._exprIntValue;
					if (e2 instanceof INT_CONST_EXPR)
						// Should either be true (1) or false (0)... same as prob_true
						prob_true = (double)((INT_CONST_EXPR)e2)._nValue;
					else if (e2 instanceof BOOL_CONST_EXPR)
						prob_true = ((BOOL_CONST_EXPR)e2)._bValue ? 1d : 0d;
					else
						throw new EvalException("Unhandled KronDelta argument: " + e2.getClass()); 
				} else if (e instanceof Bernoulli) {
					prob_true = ((REAL_CONST_EXPR)((Bernoulli)e)._exprProb)._dValue;
				} else if (e instanceof DiracDelta) {
					prob_true = ((REAL_CONST_EXPR)((DiracDelta)e)._exprRealValue)._dValue;
				} else{
					throw new EvalException("Unhandled distribution type: " + e.getClass());
				}
				if( DISPLAY_UPDATES ){
					System.out.println("RDDL state: " + _state );
					System.out.println("Updating : " + thisOne );
					System.out.println("Expression: " + expr );
					System.out.println("Subs :  " + subs );
					System.out.println("Prob. true : " + prob_true );
				}
				final boolean value = rand.nextDouble() < prob_true ? true : false;
				_nextStateMap.put( _stateVars[i], value );
			}
		}catch( EvalException e ){
			e.printStackTrace();
			System.exit(1);
		}
		final FactoredState<RDDLFactoredStateSpace> ret = new FactoredState<RDDLFactoredStateSpace>().setFactoredState( _nextStateMap );
		_nextStateMap.clear();
		return ret;
	}

	private void setStateAction(
			FactoredState<RDDLFactoredStateSpace> state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action) {
		_state.clearPVariables( _state._actions );
		_state.clearPVariables( _state._state );
		
		NavigableMap<String, Boolean> st = state.getFactoredState();
		for( Map.Entry<String, Boolean> entry : st.entrySet() ){
			String varName = entry.getKey();
			UnorderedPair<PVAR_NAME, ArrayList<LCONST>> rddlPair 
				= _rddlVars.get( varName );
			_state.setPVariableAssign(rddlPair._o1, rddlPair._o2, entry.getValue() );
		}
		
		if( action != null ){
			NavigableMap<String, Boolean> act = action.getFactoredAction();
			for( Map.Entry<String, Boolean> entry : act.entrySet() ){
				String varName = entry.getKey();
				UnorderedPair<PVAR_NAME, ArrayList<LCONST>> rddlPair 
				= _rddlVars.get( varName );
				_state.setPVariableAssign(rddlPair._o1, rddlPair._o2, entry.getValue() );
			}
		}
	}

	@Override
	public <T extends State<RDDLFactoredStateSpace>> T randomState( final Random rand ) {
		FactoredState<RDDLFactoredStateSpace> fs = null;
		do{
			final TreeMap<String, Boolean> assignments = new TreeMap<String, Boolean>();
			for( final String s : _stateVars ){
				final boolean value = rand.nextBoolean();
				assignments.put( s, value );
			}
			fs = new FactoredState<RDDLFactoredStateSpace>();
			fs.setFactoredState( assignments );
			setStateAction(fs, null );
			_constraint.setState( _state );
			try{
				_constraint.checkConstraints( );
				break;
			}catch (Exception e) {
			}
		}while( true );
		return (T) fs;
	}

	public void displayState(
			final StateViz visualizer,
			FactoredState<RDDLFactoredStateSpace> current_state,
			FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> act,
			final int time ){
//		System.out.println( "STATE : " + current_state );
//		System.out.println( "ACTION : " + act );
		
		setStateAction(current_state, act);
		if( visualizer != null ){
			visualizer.display(_state, time);
		}
	}
	//this class must build
	//atom sfor each state var then 
	//simulates factored transitions
	//by substitution

	public FactoredState<RDDLFactoredStateSpace> sampleState(
			final ADDRNode initial_state_dist,
			final Random rand ,
			final ADDManager manager ){
		NavigableMap<String, Boolean> partial_state = 
				Maps.newTreeMap( manager.sampleOneLeaf( initial_state_dist, rand ) );
		
		//HACK : GOAL fluent in crossing traffic
		
		for( final String svar : _stateVars ){
			final Boolean val = partial_state.get(svar);
			if( val == null ){
			    partial_state.put( svar, _defVal.get( svar ) );// : val );
			}
		}
		
		final FactoredState<RDDLFactoredStateSpace> partial_factored_state 
		= new FactoredState<RDDLFactoredStateSpace>().setFactoredState(partial_state);
		setStateAction( partial_factored_state, null );
		_constraint.setState( _state );
		
		try {
			_constraint.checkConstraints( false );
		}catch( Exception e ){
			e.printStackTrace();
			System.exit(1);
		}
		
		
		return partial_factored_state;
	}
	
	
}
