package rddl.mdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

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
	private Random _rand;
	private String[] _stateVars;
	private FactoredState<RDDLFactoredStateSpace> _nextState = null;
	private RDDLConstrainedMDP _constraint;
	
	public RDDLFactoredTransition( rddl.State rddlState, 
			RDDLFactoredStateSpace rddlStateSpace,
			RDDLFactoredActionSpace rddlActionSpace,
			NavigableMap<String, UnorderedPair<PVAR_NAME, ArrayList<LCONST> > > groundRDDLStateVars,
			final long seed, 
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
		_rand = new Random( seed );
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
			++i;
		}
		_constraint = new RDDLConstrainedMDP();
	}
	
	@Override
	public <T extends State<RDDLFactoredStateSpace>,
			U extends Action<RDDLFactoredStateSpace, RDDLFactoredActionSpace> >
				T sample(
			final T state,
			final U action) {
		return (T) sampleFactored( (FactoredState<RDDLFactoredStateSpace>)state, 
			(FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>) action );
	}

	@Override
	public FactoredState<RDDLFactoredStateSpace> sampleFactored(
			final FactoredState<RDDLFactoredStateSpace> state,
			final FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace> action) {
		
		try{
			setStateAction( state, action );
			_constraint.setState( _state );
			
			try {
				_constraint.checkConstraints( false );
			} catch (Exception e1) {
				e1.printStackTrace();
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
				setInNextState(_stateVars[i], prob_true );
			}
		}catch( EvalException e ){
			e.printStackTrace();
			System.exit(1);
		}
		return _nextState.copy();
	}

	private void setInNextState( String varName,
			double prob_true) {
		if( _nextState == null ){
			_nextState = new FactoredState<RDDLFactoredStateSpace>();
		}
		boolean value = _rand.nextDouble() < prob_true ? true : false;
		_nextState.setStateVariable( varName, value );
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
	public <T extends State<RDDLFactoredStateSpace>> T randomState() {
		FactoredState<RDDLFactoredStateSpace> fs = null;
		do{
			final TreeMap<String, Boolean> assignments = new TreeMap<String, Boolean>();
			for( final String s : _stateVars ){
				final boolean value = _rand.nextBoolean();
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
		System.out.println( "STATE : " + current_state );
		System.out.println( "ACTION : " + act );
		
		setStateAction(current_state, act);
		visualizer.display(_state, time);
	}
	//this class must build
	//atom sfor each state var then 
	//simulates factored transitions
	//by substitution
}
