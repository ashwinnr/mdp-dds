package mdp.define;

public class PolicyStatistics {
	private double[] mean_rewards;
	private double[] std_errors;
	private final int _numRounds;
	private int countRounds = 0;
	private int currentState = 0;
	
	public PolicyStatistics( final int numStates, final int numRounds ){
		mean_rewards = new double[numStates];
		std_errors = new double[numStates];
		_numRounds = numRounds;
	}
	
	public void addRoundStats( final double round_reward ){
		mean_rewards[currentState] += round_reward;
		std_errors[currentState] += round_reward*round_reward;
		++countRounds;
		if( countRounds == _numRounds ){
			mean_rewards[currentState] /= _numRounds;
			std_errors[currentState] = Math.sqrt( ( std_errors[currentState] - 
					_numRounds * mean_rewards[currentState]*mean_rewards[currentState] )/( _numRounds-1 ) );
			++currentState;
			countRounds = 0;
		}
	}
	
	public void printStats(){
		for( int i = 0 ; i < mean_rewards.length; ++i ){
			System.out.println( mean_rewards[i] + " " + std_errors[i] );
		}
	}
}
