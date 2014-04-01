package mdp.abstraction.parameters;

import add.ADDManager;
import dd.DDManager.DDMarginalize;
import mdp.abstraction.types.MarginalizeAbstractionType;

public class MarginalizeAbstrationParameters implements ADDAbstractionParameters< MarginalizeAbstractionType > {
	
	private  DDMarginalize _marginalize_oper;
	private String _marginalize_var;
	public MarginalizeAbstrationParameters(DDMarginalize _marginalize_oper,
			ADDManager _manager, String _marginalize_var) {
		super();
		this._marginalize_oper = _marginalize_oper;
		this._marginalize_var = _marginalize_var.intern();
	}

	public MarginalizeAbstrationParameters(DDMarginalize _marginalize_oper,
			String _marginalize_var) {
		super();
		this._marginalize_oper = _marginalize_oper;
		this._marginalize_var = _marginalize_var;
	}

	public DDMarginalize get_marginalize_oper() {
		return _marginalize_oper;
	}
	
	public String get_marginalize_var() {
		return _marginalize_var;
	}
	
}
