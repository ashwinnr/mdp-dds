package mdp.generalize.trajectory.parameters;

import java.util.Random;

import add.ADDManager;

import mdp.generalize.trajectory.type.GeneralizationType;

public abstract class GeneralizationParameters<T extends GeneralizationType> {

	public static enum GENERALIZE_PATH{
		ALL_PATHS, NONE
	}
	protected ADDManager _manager = null;
	protected GENERALIZE_PATH _genRule = GENERALIZE_PATH.NONE;
	protected Random _rand;
	
	public Random get_rand() {
		return _rand;
	}
	protected void seed_rand(final long seed) {
		this._rand = new Random( seed );
	}
	
	public GENERALIZE_PATH get_genRule() {
		return _genRule;
	}
	public ADDManager get_manager() {
		return _manager;
	}
}
