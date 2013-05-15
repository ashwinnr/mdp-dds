package rddl.mdp;

import java.util.ArrayList;
import java.util.Collections;

import rddl.RDDL.EXPR;
import rddl.RDDL.PVAR_INST_DEF;
import mdp.define.Action;
import mdp.define.ConstrainedMDP;
import mdp.define.State;

public class RDDLConstrainedMDP implements ConstrainedMDP<RDDLFactoredStateSpace, 
		RDDLFactoredActionSpace>{
	
	private rddl.State _state;

	public RDDLConstrainedMDP( ){
		super();
	}

	//NOTE : must be called before chekConstraints()
	public void setState( rddl.State state ){
		this._state = state;
	}
	
	public void checkConstraints( final boolean clear_actions ) throws Exception {
		_state.checkStateActionConstraints( null , clear_actions );
		_state = null;
	}

	@Override
	public void checkConstraints( ) throws Exception {
		checkConstraints( false );
	}
}
