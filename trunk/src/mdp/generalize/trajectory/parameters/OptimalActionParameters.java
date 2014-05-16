package mdp.generalize.trajectory.parameters;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.type.OptimalActionType;

public class OptimalActionParameters extends GeneralizationParameters<OptimalActionType>{

	private boolean all_depth;
	public enum UTYPE{
		DISJUNCT, CONJUNCT, NONE
	}
	private UTYPE _type;
	private Set<String> _actionVars;
	
	public boolean get_Alldepth(){
		return all_depth;
	}
	
	public OptimalActionParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			final boolean constrain_naively , 
			final boolean all_depth,
			final UTYPE type,
			final Set<String> actionVars ){
		super(_manager, _genRule, constrain_naively );
		this.all_depth = all_depth;
		_type = type;
		_actionVars = actionVars;
	}
	
	public UTYPE get_type() {
		return _type;
	}
	
	public Set<String> get_actionVars() {
		return Collections.unmodifiableSet( _actionVars );
	}
	
	public void set_actionVars(Set<String> _actionVars) {
		this._actionVars = _actionVars;
	}
	
}
