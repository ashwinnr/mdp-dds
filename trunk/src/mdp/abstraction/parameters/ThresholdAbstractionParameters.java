package mdp.abstraction.parameters;

import add.ADDManager;
import mdp.abstraction.types.ThresholdAbstractionType;

public class ThresholdAbstractionParameters implements ADDAbstractionParameters< ThresholdAbstractionType > {
	
	protected double _threshold;
	protected boolean _strict;

	public double get_threshold() {
		return _threshold;
	}
	
	public boolean get_strict(){
		return _strict;
	}

	public ThresholdAbstractionParameters(double _threshold, boolean _strict) {
		super();
		this._threshold = _threshold;
		this._strict = _strict;
	}
	
	
}
