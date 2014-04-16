package mdp.generalize.trajectory.parameters;

import java.util.Random;

import add.ADDManager;
import add.ADDRNode;

import mdp.generalize.trajectory.type.GeneralizationType;

public abstract class GeneralizationParameters<T extends GeneralizationType> {

	public static enum GENERALIZE_PATH{
		ALL_PATHS, NONE
	}
	protected ADDManager _manager = null;
	protected GENERALIZE_PATH _genRule = GENERALIZE_PATH.NONE;
	protected Random _rand;
	protected ADDRNode[] _valueDD;
	protected ADDRNode[] _policyDD;
	protected ADDRNode[] _visited;//optional
	protected boolean constrain_naively;
	
	public void set_visited(ADDRNode[] _visited) {
	    this._visited = _visited;
	}
	
	public ADDRNode[] get_visited() {
	    return this._visited;
	}
	
	public void set_policyDD(ADDRNode[] _policyDD) {
		this._policyDD = _policyDD;
	}
	
	public void set_valueDD(ADDRNode[] _valueDD) {
		this._valueDD = _valueDD;
	}
	
	public ADDRNode[] get_policyDD() {
		return _policyDD;
	}
	
	public ADDRNode[] get_valueDD() {
		return _valueDD;
	}
	
	public GeneralizationParameters(ADDManager _manager,
		GENERALIZE_PATH _genRule, Random _rand, final boolean constrain_naively) {
	    super();
	    this._manager = _manager;
	    this._genRule = _genRule;
	    this._rand = _rand;
	    this.constrain_naively = constrain_naively;
	}
	
	public boolean get_constrain_naively(){
		return constrain_naively;
	}
	
	public Random get_rand() {
		return _rand;
	}
	protected void seed_rand(final long seed) {
		this._rand = new Random( seed );
	}
	
	public GENERALIZE_PATH get_genRule() {
		return _genRule;
	}
	public ADDManager get_manager() {
		return _manager;
	}
	public void set_manager(ADDManager _manager) {
	    this._manager = _manager;
	}
}
