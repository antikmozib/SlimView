package io.mozib.slimview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CopyToDestinations implements Serializable {
    public List<CopyToDestination> destinations;

    public CopyToDestinations() {
        this.destinations = new ArrayList<>();
    }

    public CopyToDestinations(List<CopyToDestination> destinations) {
        this.destinations = destinations;
    }

    public static class CopyToDestination implements Serializable {
        private String destination;

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
