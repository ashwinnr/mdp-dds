package mdp.generalize.trajectory.parameters;

import java.util.Random;

import dtr.add.ADDDecisionTheoreticRegression;

import add.ADDManager;
import mdp.generalize.trajectory.type.EBL;

public class EBLParams extends GeneralizationParameters<EBL>{

	private ADDDecisionTheoreticRegression _dtr;
	private boolean on_policy;

	public EBLParams(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			final ADDDecisionTheoreticRegression dtr, 
			final boolean constrain_naively ,
			final boolean on_policy ) {
		super(_manager, _genRule, constrain_naively );
		_dtr = dtr;
		this.on_policy = on_policy;
	}
	
	public boolean getOn_policy() {
		return on_policy;
	}



	public void setOn_policy(boolean on_policy) {
		this.on_policy = on_policy;
	}



	public void set_dtr(ADDDecisionTheoreticRegression _dtr) {
		this._dtr = _dtr;
	}
	
	public ADDDecisionTheoreticRegression get_dtr() {
		return _dtr;
	}


}
