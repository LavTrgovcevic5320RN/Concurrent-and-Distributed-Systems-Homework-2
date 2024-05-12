package app;

import java.io.Serializable;
import java.util.Objects;

public class SnapshotIndicator implements Serializable {

    private static final long serialVersionUID = -9160977362084426283L;
    private Integer initiatorId;
    private Integer snapshotId;

    public SnapshotIndicator(Integer initiatorId, Integer snapshotId) {
        this.initiatorId = initiatorId;
        this.snapshotId = snapshotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SnapshotIndicator that)) return false;
        return Objects.equals(initiatorId, that.initiatorId) && Objects.equals(snapshotId, that.snapshotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initiatorId, snapshotId);
    }

    @Override
    public String toString() {
        return "SnapshotIndicator <initiatorId: " + initiatorId + ", snap: " + snapshotId + ">";
    }

    public Integer getInitiatorId() {
        return initiatorId;
    }

    public Integer getSnapshotId() {
        return snapshotId;
    }

}
