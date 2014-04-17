package mdp.generalize.trajectory.parameters;

import java.util.Random;

import dtr.add.ADDDecisionTheoreticRegression;

import add.ADDManager;
import mdp.generalize.trajectory.type.EBL;

public class EBLParams extends GeneralizationParameters<EBL>{

	private ADDDecisionTheoreticRegression _dtr;

	public EBLParams(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			Random _rand, final ADDDecisionTheoreticRegression dtr, 
			final boolean constrain_naively ) {
		super(_manager, _genRule, _rand, constrain_naively );
		_dtr = dtr;
	}
	
	public void set_dtr(ADDDecisionTheoreticRegression _dtr) {
		this._dtr = _dtr;
	}
	
	public ADDDecisionTheoreticRegression get_dtr() {
		return _dtr;
	}


}
