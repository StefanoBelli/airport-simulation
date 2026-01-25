package mbpmcsn.routing;

import mbpmcsn.center.Center;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.entity.Job;
import static mbpmcsn.core.Constants.IMPROVED_P_FAST_TRACK;

/*
 * Decides if the job goes to the Fast Track lane
 * or the standard X-Ray lane based on probability P_FAST_TRACK
 */
public final class VarchiRouting implements NetworkRoutingPoint {

    private final Center fastTrack;
    private final Center xRayStandard;
    private final int streamIndex;

    public VarchiRouting(Center fastTrack, Center xRayStandard, int streamIndex) {
        this.fastTrack = fastTrack;
        this.xRayStandard = xRayStandard;
        this.streamIndex = streamIndex;
    }

    @Override
    public Center getNextCenter(Rngs r, Job job) {
        r.selectStream(streamIndex);

        boolean isFastTrackBeingUsed = r.random() < IMPROVED_P_FAST_TRACK;
        job.setFastTrackBeingUsed(isFastTrackBeingUsed);

        return isFastTrackBeingUsed ? fastTrack : xRayStandard;
    }
}
