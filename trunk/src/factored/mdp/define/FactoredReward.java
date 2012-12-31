package factored.mdp.define;

import mdp.define.Reward;

public interface FactoredReward<S extends FactoredStateSpace, A extends FactoredActionSpace<S>>
	extends Reward<S,A>{
	
}
