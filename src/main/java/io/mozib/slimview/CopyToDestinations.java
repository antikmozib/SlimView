/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the list of locations/destinations the user has chosen in the Copy File To window
 */
public class CopyToDestinations implements Serializable {
    private List<CopyToDestination> destinations = new ArrayList<>();

    public CopyToDestinations() {
    }

    public List<CopyToDestination> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<CopyToDestination> destinations) {
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

        public void setDestination(String destination) {
            this.destination = destination;
        }
    }
}
