package mdp.abstraction.parameters;

import add.ADDManager;
import mdp.abstraction.types.ApricoddAbstractionType;
import dd.DDManager.APPROX_TYPE;

public class ApricoddAbstractionParameters implements ADDAbstractionParameters< ApricoddAbstractionType >{

	private double _apricodd_epsilon;
	private APPROX_TYPE _approx_type;
	private ADDManager _manager;
	
	public ApricoddAbstractionParameters(
			final double _apricodd_epsilon,
			final APPROX_TYPE _approx_type,
			final ADDManager manager ) {
		super();
		this._manager = manager;
		this._apricodd_epsilon = _apricodd_epsilon;
		this._approx_type = _approx_type;
	}
	
}
