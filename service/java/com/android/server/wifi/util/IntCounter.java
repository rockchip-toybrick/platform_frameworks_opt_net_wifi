/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wifi.util;

import android.util.SparseIntArray;

import com.android.server.wifi.proto.nano.WifiMetricsProto.Int32Count;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * Utility class for counting occurrences of int keys using an int counter.
 * Note: this class can also be used for counting occurrences of enum values. Just define a new
 * Protobuf message, and call {@link #toProto(Class, ProtobufConverter)} with a
 * {@link ProtobufConverter} that populates your custom Protobuf message type.
 */
public class IntCounter extends SparseIntArray implements Iterable<IntCounter.KeyCount> {

    /**
     * A class to represent the number of occurrences for an int key.
     */
    public static class KeyCount {
        public int key;
        public int count;

        public KeyCount(int key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    /**
     * Calls to {@link #add(int, int)}/{@link #increment(int)} for all keys < keyLowerBound are
     * instead attributed to keyLowerBound.
     */
    public final int keyLowerBound;
    /**
     * Calls to {@link #add(int, int)}/{@link #increment(int)} for all keys > keyUpperBound are
     * instead attributed to keyUpperBound.
     */
    public final int keyUpperBound;

    public IntCounter() {
        this(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Clamps keys to the range between keyLowerBound and keyUpperBound. See {@link #keyLowerBound}
     * and {@link #keyUpperBound}.
     */
    public IntCounter(int keyLowerBound, int keyUpperBound) {
        this.keyLowerBound = keyLowerBound;
        this.keyUpperBound = keyUpperBound;
    }

    /**
     * Increments the count of a key by 1.
     */
    public void increment(int key) {
        add(key, 1);
    }

    /**
     * Increments the count of a key by <code>count</code>.
     */
    public void add(int key, int count) {
        key = Math.max(keyLowerBound, Math.min(key, keyUpperBound));
        int curCount = get(key); // returns 0 if key not found
        put(key, curCount + count);
    }

    /**
     * Iterates over all (key, count) pairs.
     */
    @Override
    public Iterator<KeyCount> iterator() {
        return new Iterator<KeyCount>() {
            private int mIndex = 0;

            @Override
            public boolean hasNext() {
                return mIndex < size();
            }

            @Override
            public KeyCount next() {
                KeyCount kc = new KeyCount(keyAt(mIndex), valueAt(mIndex));
                mIndex++;
                return kc;
            }
        };
    }

    /**
     * Converter function that converts a single (key, count) pair to a Protobuf object.
     * @param <T> the type of the Protobuf output.
     */
    public interface ProtobufConverter<T> {
        /**
         * Converter function that converts a single (key, count) pair to a Protobuf object.
         * @param key the key that we are counting occurrences for
         * @param count the number of occurrences for this key
         * @return the Protobuf output
         */
        T convert(int key, int count);
    }

    /**
     * Converts this object to a custom Protobuf representation.
     * @param protoClass the class object for the Protobuf type.
     * @param converter a conversion function.
     * @param <T> the type of the Protobuf output.
     * @return an array of Protobuf representation of buckets generated by the converter function.
     */
    public <T> T[] toProto(Class<T> protoClass, ProtobufConverter<T> converter) {
        @SuppressWarnings("unchecked")
        T[] output = (T[]) Array.newInstance(protoClass, size());
        int i = 0;
        for (KeyCount kc : this) {
            output[i] = converter.convert(kc.key, kc.count);
            i++;
        }
        return output;
    }

    /**
     * Converts this object to a standard Protobuf representation.
     */
    public Int32Count[] toProto() {
        return toProto(Int32Count.class, (key, count) -> {
            Int32Count entry = new Int32Count();
            entry.key = key;
            entry.count = count;
            return entry;
        });
    }
}
