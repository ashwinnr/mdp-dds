package mdp.abstraction.parameters;

import add.ADDManager;
import mdp.abstraction.types.ThresholdAbstractionType;

public class ThresholdAbstractionParameters implements ADDAbstractionParameters< ThresholdAbstractionType > {
	
	protected double _threshold;
	protected boolean _strict;
	protected ADDManager _manager;
	
}
