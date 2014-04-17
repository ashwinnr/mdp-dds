package mdp.generalize.trajectory.parameters;

import java.util.Random;

import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.type.OptimalActionType;

public class OptimalActionParameters extends GeneralizationParameters<OptimalActionType>{

	private boolean all_depth;
	public enum UTYPE{
		DISJUNCT, CONJUNCT, NONE
	}
	private UTYPE _type;
	
	public boolean get_Alldepth(){
		return all_depth;
	}
	
	public OptimalActionParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			Random _rand ,
			final boolean constrain_naively , 
			final boolean all_depth,
			final UTYPE type ){
		super(_manager, _genRule, _rand, constrain_naively );
		this.all_depth = all_depth;
		_type = type;
	}
	
	public UTYPE get_type() {
		return _type;
	}
	
}
