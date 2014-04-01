package mdp.abstraction.parameters;

import add.ADDManager;
import mdp.abstraction.types.ApricoddAbstractionType;
import dd.DDManager.APPROX_TYPE;

public class ApricoddAbstractionParameters implements ADDAbstractionParameters< ApricoddAbstractionType >{

	private double _apricodd_epsilon;
	private APPROX_TYPE _approx_type;
	
	public ApricoddAbstractionParameters(double _apricodd_epsilon,
			APPROX_TYPE _approx_type ) {
		super();
		this._apricodd_epsilon = _apricodd_epsilon;
		this._approx_type = _approx_type;
	}
	
	public APPROX_TYPE get_approx_type() {
		return this._approx_type;
	}
	
	public double get_apricodd_epsilon() {
		return this._apricodd_epsilon;
	}
}
