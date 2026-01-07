package mbpmcsn.center.abstracts;

import mbpmcsn.desbook.Rngs;

public interface ArrivalProcess {
	double getArrival();
	void resetRngs(Rngs rngs);
}

