package mdp.abstraction.parameters;

import add.ADDManager;
import dd.DDManager.DDMarginalize;
import mdp.abstraction.types.MarginalizeAbstractionType;

public class MarginalizeAbstrationParameters implements ADDAbstractionParameters< MarginalizeAbstractionType > {
	
	protected DDMarginalize _marginalize_oper;
	protected ADDManager _manager;
	protected String _marginalize_var;
	public MarginalizeAbstrationParameters(DDMarginalize _marginalize_oper,
			ADDManager _manager, String _marginalize_var) {
		super();
		this._marginalize_oper = _marginalize_oper;
		this._manager = _manager;
		this._marginalize_var = _marginalize_var;
	}
	
}
