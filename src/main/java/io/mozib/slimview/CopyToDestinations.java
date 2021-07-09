package io.mozib.slimview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the list of locations/destinations the user has chosen in the Copy File To window
 */
public class CopyToDestinations implements Serializable {
    public List<CopyToDestination> destinations;

    public CopyToDestinations() {
        this.destinations = new ArrayList<>();
    }

    // required for serialization
    public CopyToDestinations(List<CopyToDestination> destinations) {
        this.destinations = destinations;
    }

    /**
     * Represents a single location/destination
     */
    public static class CopyToDestination implements Serializable {
        private String destination;

        // required for serialization
        public CopyToDestination(String destination) {
            this.destination = destination;
        }

        public CopyToDestination() {
        }

        @Override
        public String toString() {
            return destination;
        }

        public String getDestination() {
            return destination;
        }
    }
}
