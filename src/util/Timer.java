/**
 * Utilities: A simple Timer class.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 9/1/03
 *
 **/

package util;

public class Timer {

	// Internal timing stats
	public long _lTime;
	public long _lElapsedTime;
	
	// Starts timer upon creation
	public Timer() {
		ResetTimer();
	}
	
	// Reset and start the timer
	public void ResetTimer() {
		_lTime = System.currentTimeMillis();
		_lElapsedTime = 0;
	}
	
	public void ResumeTimer() {
		_lTime = System.currentTimeMillis();
	}
	
	public long PauseTimer() {
		_lElapsedTime += System.currentTimeMillis() - _lTime;
		return _lElapsedTime;
	}

	public long StopTimer() {
		_lElapsedTime = System.currentTimeMillis() - _lTime;
		return _lElapsedTime;
	}

	public long GetTimeSoFar() {
		return System.currentTimeMillis() - _lTime;
	}

	public long GetTimeSoFarAndReset() {
		long elapsed = System.currentTimeMillis() - _lTime;
		ResetTimer();
		return elapsed;
	}

	public long GetElapsedTime() {
		return _lElapsedTime;
	}

	@Override
	public String toString() {
		return "Timer [_lTime=" + this._lTime + ", _lElapsedTime="
				+ this._lElapsedTime + "]";
	}

	/**
	 * @return
	 */
	public double GetElapsedTimeInMinutes() {
		return GetElapsedTime()/(1000d*60);
	}

	/**
	 * @return
	 */
	public double GetTimeSoFarAndResetInMinutes() {
		long l = GetTimeSoFarAndReset();
		return l/(1000d*60);
	}

	/**
	 * @return
	 */
	public double GetElapsedTimeInMinutesAndReset() {
		double ret = GetElapsedTimeInMinutes();
		ResetTimer();
		return ret;
	}

}
