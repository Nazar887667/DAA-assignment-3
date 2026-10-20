package graph;

import java.util.Arrays;

public class BinaryMinHeap {
    private long[] keys = new long[16];
    private int[] a = new int[16];
    private int[] b = new int[16];
    private int size = 0;

    public long comparisons = 0;
    public long pushes = 0;
    public long pops = 0;

    public long topKey;
    public int topA;
    public int topB;

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    public void push(long key, int pa, int pb) {
        pushes++;
        if (size == keys.length) {
            int cap = size * 2;
            keys = Arrays.copyOf(keys, cap);
            a = Arrays.copyOf(a, cap);
            b = Arrays.copyOf(b, cap);
        }
        int i = size++;
        keys[i] = key; a[i] = pa; b[i] = pb;
        while (i > 0) {
            int p = (i - 1) >>> 1;
            comparisons++;
            if (keys[i] < keys[p]) { swap(i, p); i = p; } else break;
        }
    }

    public void pop() {
        pops++;
        topKey = keys[0]; topA = a[0]; topB = b[0];
        size--;
        if (size > 0) {
            keys[0] = keys[size]; a[0] = a[size]; b[0] = b[size];
            int i = 0;
            while (true) {
                int l = 2 * i + 1, r = l + 1, m = i;
                if (l < size) { comparisons++; if (keys[l] < keys[m]) m = l; }
                if (r < size) { comparisons++; if (keys[r] < keys[m]) m = r; }
                if (m == i) break;
                swap(i, m); i = m;
            }
        }
    }

    private void swap(int i, int j) {
        long k = keys[i]; keys[i] = keys[j]; keys[j] = k;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        t = b[i]; b[i] = b[j]; b[j] = t;
    }
}
