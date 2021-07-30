package com.api.swarm.assignement;

import java.math.BigInteger;
import java.util.Arrays;

public class Permutation <T> {
    private T[] arr;
    private final int[] permSwappings;

    /** Instantiate a Permutation over an array of objects. The objects must be Cloneable. */
    public Permutation(T[] arr) {
        this(arr,arr.length);
    }

    /** Instantiate a Permutation over an array of objects, in blocks of permSize. The objects must be Cloneable. */
    public Permutation(T[] arr, int permSize) {
        this.arr = arr.clone();
        this.permSwappings = new int[permSize];
        for(int i = 0;i < permSwappings.length;i++) {
            permSwappings[i] = i;
        }
    }

    /** Gets the next combination. */
    public T[] next() {
        if (arr == null) {
            return null;
        }
        T[] res = Arrays.copyOf(arr, permSwappings.length);
        //Prepare next
        int i = permSwappings.length-1;
        while (i >= 0 && permSwappings[i] == arr.length - 1) {
            swap(i, permSwappings[i]); //Undo the swap represented by permSwappings[i]
            permSwappings[i] = i;
            i--;
        }

        if (i < 0) {
            arr = null;
        } else {
            int prev = permSwappings[i];
            swap(i, prev);
            int next = prev + 1;
            permSwappings[i] = next;
            swap(i, next);
        }
        return res;
    }

    private void swap(int i, int j) {
        T tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    /** Returns the number of possible permutations. */
    public BigInteger size() {
        // n! / (n - k)!
        return Permutation.factorial(this.arr.length).divide(Permutation.factorial(this.arr.length - this.permSwappings.length));
    }

    /** Provides the factorial of a number. */
    private static BigInteger factorial(int n) {

        BigInteger res = BigInteger.valueOf(1);
        for (int i = 1; i <= n; i++) {
            res = res.multiply(BigInteger.valueOf(i));
        }
        return res;
    }
}
