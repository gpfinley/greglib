package greglib.util;

import java.util.*;

/**
 * Created by greg on 10/1/16.
 */
public class StepwiseClusteringSlow<T> {

    private final List<ClusterPointer> clusters;
    private final List<T> items;
    private final List<Link> links;
    private final Map<T, Integer> indexOf;
    private Map<T, Set<T>> myClusterMap;

    public StepwiseClusteringSlow(Iterable<T> initItems) {
        clusters = new ArrayList<>();
        items = new ArrayList<>();
        links = new ArrayList<>();
        indexOf = new HashMap<>();
        for (T item : initItems) {
            indexOf.put(item, items.size());
            items.add(item);
            clusters.add(new ClusterPointer(Collections.singleton(item)));
        }
    }

    public void addLink(T from, T to, double score) {
        if (indexOf.containsKey(from) && indexOf.containsKey(to)) {
            links.add(new Link(indexOf.get(from), indexOf.get(to), score));
        }
    }

    public void cluster(int k) {
        links.sort((x, y) -> y.score.compareTo(x.score));
        int n = clusters.size();
        if (n <= k) return;
        for (Link link : links) {
            if (clusters.get(link.one).combine(clusters.get(link.two))) {
                n--;
            }
            if (n <= k) break;
        }

        myClusterMap = getClusterMap();
    }

    private Map<T, Set<T>> getClusterMap() {
        Map<T, Set<T>> itemToCluster = new HashMap<>();
        for (int i=0; i<items.size(); i++) {
            itemToCluster.put(items.get(i), clusters.get(i).getCluster());
        }
        return itemToCluster;
    }

    private class ClusterPointer {
        private Set<T> cluster;
        private ClusterPointer pointer;
        public ClusterPointer(Set<T> phrases) {
            cluster = phrases;
            pointer = null;
        }

        /**
         * Combine the clusters that these nodes are a part of
         * @param other another ClusterPointer
         * @return true if this link reduced the number of clusters
         */
        public boolean combine(ClusterPointer other) {
            if (this.getCluster() == other.getCluster()) return false;
            Set<T> union = new HashSet<>(getCluster());
            union.addAll(other.getCluster());
            ClusterPointer bothPointToThis = new ClusterPointer(union);
            setPointer(bothPointToThis);
            other.setPointer(bothPointToThis);
            return true;
        }
        private void setPointer(ClusterPointer to) {
            if (pointer == null) {
                cluster = null;         // to save memory
                pointer = to;
            } else {
                pointer.setPointer(to);
            }
        }
        public Set<T> getCluster() {
            if (pointer == null) return cluster;
            return pointer.getCluster();
        }
    }

    private static class Link {
        int one;
        int two;
        Double score;
        Link(int one, int two, Double score) {
            this.one = one;
            this.two = two;
            this.score = score;
        }
        @Override
        public String toString() {
            return one + " " + two + " " + score;
        }
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Link)) return false;
            Link linkOther = (Link) other;
            return one == linkOther.one && two == linkOther.two;
        }
        @Override
        public int hashCode() {
            return (one + " " + two + " " + score).hashCode();
        }
    }

}
