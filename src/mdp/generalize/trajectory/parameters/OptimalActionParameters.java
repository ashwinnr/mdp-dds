package mdp.generalize.trajectory.parameters;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.type.OptimalActionType;

public class OptimalActionParameters extends GeneralizationParameters<OptimalActionType>{

	public enum UTYPE{
		DISJUNCT, CONJUNCT, NONE
	}
	private Set<String> _actionVars;
	
	public OptimalActionParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			final boolean constrain_naively , 
			final Set<String> actionVars ){
		super(_manager, _genRule, constrain_naively );
		_actionVars = actionVars;
	}
	
	public Set<String> get_actionVars() {
		return Collections.unmodifiableSet( _actionVars );
	}
	
	public void set_actionVars(Set<String> _actionVars) {
		this._actionVars = _actionVars;
	}
	
}
