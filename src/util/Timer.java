/**
 * Utilities: A simple Timer class.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 9/1/03
 *
 **/

package util;

import java.io.IOException;

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
		_lTime = System.nanoTime();
		_lElapsedTime = 0;
	}
	
	public void ResumeTimer() {
		_lTime = System.nanoTime();
	}
	
	public long PauseTimer() {
		_lElapsedTime += System.nanoTime() - _lTime;
		return _lElapsedTime;
	}

	public long StopTimer() {
		_lElapsedTime = System.nanoTime() - _lTime;
		return _lElapsedTime;
	}

	public long GetTimeSoFar() {
		return System.nanoTime() - _lTime;
	}

	public long GetTimeSoFarAndReset() {
		long elapsed = System.nanoTime() - _lTime;
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
		return nanoToMinutes( GetElapsedTime() );
	}

	/**
	 * @return
	 */
	public double GetTimeSoFarAndResetInMinutes() {
		long l = GetTimeSoFarAndReset();
		return nanoToMinutes( l );
	}

	private double nanoToMinutes(long l) {
		//1 ns = 10^-9 s
		//l ns = l*10^-9/60 mins
		return l*(1e-9d)/60;
	}

	public static void main( String[] args ){
		Timer t = new Timer();
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		t.PauseTimer();
		System.out.println( t.GetElapsedTimeInMinutes() );
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
