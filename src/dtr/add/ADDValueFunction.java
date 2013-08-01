package dtr.add;

import java.util.Collection;
import java.util.NavigableMap;

import dtr.SymbolicValueFunction;

import rddl.mdp.RDDL2ADD;
import rddl.mdp.RDDLFactoredActionSpace;
import rddl.mdp.RDDLFactoredStateSpace;

import mdp.define.Action;
import mdp.define.State;
import factored.mdp.define.FactoredAction;
import factored.mdp.define.FactoredActionSpace;
import factored.mdp.define.FactoredReward;
import factored.mdp.define.FactoredState;
import factored.mdp.define.FactoredStateSpace;
import factored.mdp.define.FactoredTransition;
import factored.mdp.define.FactoredValueFunction;
import util.Pair;
import add.ADDINode;
import add.ADDLeaf;
import add.ADDManager;
import add.ADDNode;
import add.ADDRNode;

public class ADDValueFunction 
	extends SymbolicValueFunction<ADDNode, ADDRNode, ADDINode, ADDLeaf, RDDLFactoredStateSpace, 
		RDDLFactoredActionSpace > {
	
	public ADDValueFunction( ADDRNode vFn, NavigableMap<? extends FactoredAction<RDDLFactoredStateSpace, 
			RDDLFactoredActionSpace>, ADDRNode> qFn, 
			ADDRNode jointQFn,
			ADDManager man ){
		set_valueFn(vFn);
		_qFn = qFn;
		_manager = man;
		set_jointQFn(jointQFn);
	}
	
	public void addPermenant( ){
		int size = ( _qFn == null ) ? 0 : _qFn.size();
		ADDRNode[] arr = new ADDRNode[1+( get_jointQFn() != null && _qFn == null ? 1 : 0 )
		                              	+ ( get_jointQFn() == null && _qFn != null ? size : 0 )];
		arr[0] = get_valueFn();
		if( get_jointQFn() != null && _qFn == null ){
			arr[1] = get_jointQFn();
		}else{
			int i = 1;
			for( ADDRNode dr : _qFn.values() ){
				arr[i++] = dr;
			}
		}
		_manager.addPermenant(arr);
	}

	@Override
	public <T extends FactoredState<RDDLFactoredStateSpace>> 
		Pair<Double, Double> getFactoredStateValue(T state){
		NavigableMap<String, Boolean> factored_state = state.getFactoredState();
		ADDRNode leaf = _manager.evaluate(get_valueFn(), factored_state);
		if( leaf.getNode() instanceof ADDINode ){
			try{
				throw new Exception("Evaluate did not give leaf");
			}catch( Exception e ){
				e.printStackTrace();
			}
		}
		return ((ADDLeaf)(leaf.getNode())).getLeafValues();
	}

	@Override
	public <T extends FactoredState<RDDLFactoredStateSpace>, U extends 
		FactoredAction<RDDLFactoredStateSpace, RDDLFactoredActionSpace>> 
			Pair<Double, Double> getFactoredStateActionValue(T state, U action){
	
		NavigableMap<String, Boolean> state_assign = state.getFactoredState();
		NavigableMap<String, Boolean> action_assign = action.getFactoredAction();
		state_assign.putAll( action_assign );
		
		ADDRNode qFun = ( _qFn == null ) ? null : _qFn.get( action_assign );
		if( qFun == null ){
			qFun = ( get_jointQFn() == null ) ? null : get_jointQFn();
		}
		
		ADDRNode leaf = _manager.evaluate( qFun, state_assign );
		if( leaf.getNode() instanceof ADDINode ){
			try{
				throw new Exception("Evaluate did not give leaf");
			}catch( Exception e ){
				e.printStackTrace();
			}
		}
		return ((ADDLeaf)(leaf.getNode())).getLeafValues();
	}

	@Override
	public <T extends State<RDDLFactoredStateSpace>> Pair<Double, Double> 
		getStateValue( T state) {
		return getFactoredStateValue((FactoredState<RDDLFactoredStateSpace>)state);
	}

	@Override
	public <T extends State<RDDLFactoredStateSpace>, U extends 
		Action<RDDLFactoredStateSpace, RDDLFactoredActionSpace>> 
			Pair<Double, Double> getStateActionValue(T state, U action) {
		return getFactoredStateActionValue( (FactoredState<RDDLFactoredStateSpace>)state, 
				(FactoredAction<RDDLFactoredStateSpace,
						RDDLFactoredActionSpace>)action );
	}
	
	public ADDRNode getValueFn(){
		return get_valueFn();
	}

	@Override
	public void showValueFunctions() {
		int size = ( _qFn == null ) ? 0 : _qFn.size();
		ADDRNode[] arr = new ADDRNode[1+( get_jointQFn() != null && _qFn == null ? 1 : 0 )
		                              	+ ( get_jointQFn() == null && _qFn != null ? size : 0 )];
		arr[0] = get_valueFn();
		if( get_jointQFn() != null && _qFn == null ){
			arr[1] = get_jointQFn();
		}else{
			int i = 1;
			for( ADDRNode dr : _qFn.values() ){
				arr[i++] = dr;
			}
		}
		
		_manager.showGraph( arr );
	}

	@Override
	public void throwAwayQFunctions() {
		if( _qFn != null ){
			ADDRNode[] arr = new ADDRNode[ _qFn.size() ];
			arr = _qFn.values().toArray( arr );
			_manager.removePermenant( arr );
			for( ADDRNode r : arr ){
				r.nullify();
				r = null;
			}
		}
		if( get_jointQFn() != null ){
			_manager.removePermenant( get_jointQFn() );
			get_jointQFn().nullify();
			set_jointQFn(null);
		}
	}

}
