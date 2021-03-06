package greglib.util;

import java.util.*;

/**
 * General-use Comparator for sorting based on map values. Will fall back to keys if values are equivalent
 * Be sure that the values implement Comparable (will probably be Integer or Double)
 * Created by gpfinley on 3/1/16.
 */
public class ByValue<K, V extends Comparable<V>> implements Comparator<K> {
    private final Map<K, V> map;
    private final boolean reverse;

    public ByValue(Map<K, V> map) {
        this(map, false);
    }

    public ByValue(Map<K, V> map, boolean reverse) {
        this.map = map;
        this.reverse = reverse;
    }

    public int compare(K o1, K o2) {
        int cmp = map.get(o1).compareTo(map.get(o2));
        if(cmp == 0) {
            // Is there a way to be sure that K is Comparable<K>?
            if(o1 instanceof Comparable)
                cmp = ((Comparable<K>)o1).compareTo(o2);
            else return 0;
        }
        return reverse ? -cmp : cmp;
    }

    /**
     * Transform a map into a value-sorted version of the same contents.
     * todo: test
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V extends Comparable<V>> SortedMap<K, V> getValueSortedVersion(Map<K, V> map) {
        SortedMap<K, V> newMap = new TreeMap<>(new ByValue<>(map));
        map.forEach(newMap::put);
        return newMap;
    }
}
