/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 

Contributors:
    ...
**********************************************************************/
package org.datanucleus.util;

import java.util.LinkedList;

/**
 * Math Utilities.
 */
public class MathUtils
{
    /**
     * Simple Moving Average
     */
    public static class SMA
    {
        private LinkedList values = new LinkedList();

        private int length;

        private double sum = 0;

        private double average = 0;
        
        /**
         * 
         * @param length the maximum length
         */
        public SMA(int length)
        {
            if (length <= 0)
            {
                throw new IllegalArgumentException("length must be greater than zero");
            }
            this.length = length;
        }

        public double currentAverage()
        {
            return average;
        }

        /**
         * Compute the moving average.
         * Synchronised so that no changes in the underlying data is made during calculation.
         * @param value The value
         * @return The average
         */
        public synchronized double compute(double value)
        {
            if (values.size() == length && length > 0)
            {
                sum -= ((Double) values.getFirst()).doubleValue();
                values.removeFirst();
            }
            sum += value;
            values.addLast(new Double(value));
            average = sum / values.size();
            return average;
        }
    }
}