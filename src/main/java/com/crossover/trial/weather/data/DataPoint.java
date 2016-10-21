package com.crossover.trial.weather.data;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A collected point, including some information about the range of collected values
 *
 * @author code test administrator
 */
public class DataPoint {

    /**
     * the mean of the observations
     */
    private double mean = 0.0;

    /**
     * 1st quartile -- useful as a lower bound
     *
     * @return first quartile
     */
    private int first = 0;

    /**
     * 2nd quartile -- median value
     */
    private int second = 0;

    /**
     * 3rd quartile value -- less noisy upper value
     */
    private int third = 0;

    /**
     * the total number of measurements
     */
    private int count = 0;

    /**
     * private constructor, use the builder to create this object
     */
    private DataPoint(int first, int second, int mean, int third, int count) {
        this.setFirst(first);
        this.setMean(mean);
        this.setSecond(second);
        this.setThird(third);
        this.setCount(count);
    }


    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }


    public int getFirst() {
        return first;
    }


    public void setFirst(int first) {
        this.first = first;
    }


    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }


    public int getThird() {
        return third;
    }

    public void setThird(int third) {
        this.third = third;
    }


    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }

    public boolean equals(Object that) {
        DataPoint newDp = (DataPoint) that;
        return this.mean == newDp.mean &&
                this.first == newDp.first &&
                this.second == newDp.second &&
                this.third == newDp.third &&
                this.count == newDp.count;
    }

    static public class Builder {
        int first;
        int mean;
        int median;
        int last;
        int count;

        public Builder() {
        }

        public Builder withFirst(int first) {
            this.first = first;
            return this;
        }

        public Builder withMean(int mean) {
            this.mean = mean;
            return this;
        }

        public Builder withMedian(int median) {
            this.median = median;
            return this;
        }

        public Builder withCount(int count) {
            this.count = count;
            return this;
        }

        public Builder withLast(int last) {
            this.last = last;
            return this;
        }

        public DataPoint build() {
            return new DataPoint(this.first, this.mean, this.median, this.last, this.count);
        }
    }
}
