package mdp.generalize.trajectory.parameters;

import java.util.List;
import java.util.Random;

import dtr.add.ADDDecisionTheoreticRegression;

import add.ADDManager;
import add.ADDRNode;
import mdp.generalize.trajectory.type.RewardGeneralizationType;
import mdp.generalize.trajectory.type.ValueGeneralizationType;

public class RewardGeneralizationParameters extends GeneralizationParameters<RewardGeneralizationType> {

	private List<ADDRNode> _rewards;
	
	public RewardGeneralizationParameters(
			ADDManager _manager,
			mdp.generalize.trajectory.parameters.GeneralizationParameters.GENERALIZE_PATH _genRule,
			final List<ADDRNode> rewards ,
			final boolean constrain_naively, final List<ADDRNode> max_rewards ) {
		super(_manager, _genRule, constrain_naively );
		_rewards = rewards;
	}

	public List<ADDRNode> get_rewards() {
		return _rewards;
	}
	
	public void set_rewards(List<ADDRNode> _rewards) {
		this._rewards = _rewards;
	}
	
}
