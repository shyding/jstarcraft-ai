package com.jstarcraft.ai.math.structure;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.apache.commons.math3.util.FastMath;

import com.jstarcraft.core.utility.Float2FloatKeyValue;
import com.jstarcraft.core.utility.KeyValue;

import it.unimi.dsi.fastutil.floats.Float2IntMap;
import it.unimi.dsi.fastutil.floats.Float2IntOpenHashMap;

/**
 * 标量迭代器
 * 
 * <pre>
 * 利用Float.intBitsToFloat与Float.floatToIntBits(value)可以实现int与float转换,并且防止精度丢失.
 * </pre>
 * 
 * @author Birdy
 *
 */
public interface ScalarIterator<T extends MathScalar> extends MathIterator<T> {

    /**
     * 缩放所有标量的值
     * 
     * @param value
     * @return
     */
    ScalarIterator<T> scaleValues(float value);

    /**
     * 设置所有标量的值
     * 
     * @param value
     * @return
     */
    ScalarIterator<T> setValues(float value);

    /**
     * 偏移所有标量的值
     * 
     * @param value
     * @return
     */
    ScalarIterator<T> shiftValues(float value);

    /**
     * 获取边界
     * 
     * @param absolute
     * @return
     */
    default KeyValue<Float, Float> getBoundary(boolean absolute) {
        float maximum = Float.NEGATIVE_INFINITY;
        float minimum = Float.POSITIVE_INFINITY;
        float value = 0F;
        if (absolute) {
            for (MathScalar term : this) {
                value = FastMath.abs(term.getValue());
                if (maximum < value) {
                    maximum = value;
                }
                if (minimum > value) {
                    minimum = value;
                }
            }
        } else {
            for (MathScalar term : this) {
                value = term.getValue();
                if (maximum < value) {
                    maximum = value;
                }
                if (minimum > value) {
                    minimum = value;
                }
            }
        }
        return new KeyValue<>(minimum, maximum);
    }

    /**
     * 获取计数
     * 
     * @param absolute
     * @return
     */
    default Float2IntMap getCount(boolean absolute) {
        Float2IntMap counts = new Float2IntOpenHashMap();
        if (absolute) {
            for (MathScalar term : this) {
                float key = FastMath.abs(term.getValue());
                int value = counts.getOrDefault(key, 0);
                counts.put(key, value + 1);
            }
        } else {
            for (MathScalar term : this) {
                float key = term.getValue();
                int value = counts.getOrDefault(key, 0);
                counts.put(key, value + 1);
            }
        }
        return counts;
    }

    /**
     * 获取峰度(kurtosis)
     * 
     * @param mean
     * @return
     */
    default float getKurtosis(float sum) {
        int know = getKnownSize();
        int unknow = getUnknownSize();
        int length = know + unknow;
        float mean = sum / length;

        float kurtosis = 0F;
        for (MathScalar term : this) {
            kurtosis += Math.pow(term.getValue() - mean, 4F);
        }
        // TODO 处理跳过0的部分
        kurtosis += Math.pow(-mean, 4F) * unknow;
        // TODO 处理跳过0的部分
        float variance = (getVariance(mean) + mean * mean * unknow) / length;
        kurtosis = (float) (kurtosis / (Math.pow(variance, 2F) * (length - 1F)) - 3F);
        return kurtosis;
    }

    /**
     * 获取中位数
     * 
     * @return
     */
    // TODO 准备重构为基于Float2IntMap计算中位数
    default float getMedian(boolean absolute) {
        int count = 0;
        PriorityQueue<Float> minimumQueue = new PriorityQueue<>(new Comparator<Float>() {

            @Override
            public int compare(Float left, Float right) {
                return left.compareTo(right);
            }

        });
        PriorityQueue<Float> maximumQueue = new PriorityQueue<>(new Comparator<Float>() {

            @Override
            public int compare(Float left, Float right) {
                return right.compareTo(left);
            }

        });
        if (absolute) {
            for (MathScalar term : this) {
                if (count % 2 == 0) {
                    maximumQueue.offer(FastMath.abs(term.getValue()));
                    float value = maximumQueue.poll();
                    minimumQueue.offer(value);
                } else {
                    minimumQueue.offer(FastMath.abs(term.getValue()));
                    float value = minimumQueue.poll();
                    maximumQueue.offer(value);
                }
                count++;
            }
        } else {
            for (MathScalar term : this) {
                if (count % 2 == 0) {
                    maximumQueue.offer(term.getValue());
                    float value = maximumQueue.poll();
                    minimumQueue.offer(value);
                } else {
                    minimumQueue.offer(term.getValue());
                    float value = minimumQueue.poll();
                    maximumQueue.offer(value);
                }
                count++;
            }
        }
        if (count % 2 == 0) {
            return new Float((minimumQueue.peek() + maximumQueue.peek())) / 2F;
        } else {
            return new Float(minimumQueue.peek());
        }
    }

    /**
     * 获取范数
     * 
     * @param power
     * @return
     */
    default float getNorm(float power, boolean root) {
        // TODO 此处对称矩阵可能会存在错误,需要Override
        // 处理power为0的情况
        if (power == 0F) {
            return getElementSize();
        } else {
            float norm = 0F;
            if (power == 1F) {
                for (MathScalar term : this) {
                    norm += FastMath.abs(term.getValue());
                }
                return norm;
            }
            if (power == 2F) {
                for (MathScalar term : this) {
                    norm += term.getValue() * term.getValue();
                }
                return (float) Math.sqrt(norm);
            }
            // 处理power为2的倍数次方的情况
            if ((int) power == power && power % 2F == 0F) {
                for (MathScalar term : this) {
                    norm += FastMath.pow(term.getValue(), power);
                }
            } else {
                for (MathScalar term : this) {
                    norm += FastMath.pow(FastMath.abs(term.getValue()), power);
                }
            }
            return root ? (float) FastMath.pow(norm, 1F / power) : norm;
        }
    }

    /**
     * 获取偏度(skewness)
     * 
     * @param mean
     * @return
     */
    default float getSkewness(float sum) {
        int know = getKnownSize();
        int unknow = getUnknownSize();
        int length = know + unknow;
        float mean = sum / length;

        float skewness = 0F;
        for (MathScalar term : this) {
            skewness += Math.pow(term.getValue() - mean, 3F);
        }
        // TODO 处理跳过0的部分
        skewness += Math.pow(-mean, 3F) * unknow;
        // TODO 处理跳过0的部分
        float variance = (getVariance(mean) + mean * mean * unknow) / length;
        skewness = (float) (skewness / (pow(Math.sqrt(variance), 3F) * (length - 1F)));
        if (length >= 3) {
            // We can use the bias corrected formula
            return (float) (sqrt(length * (length - 1F)) / (length - 2F) * skewness);
        }
        return skewness;
    }

    /**
     * 获取总数
     * 
     * @param absolute
     * @return
     */
    default float getSum(boolean absolute) {
        // TODO 此处对称矩阵可能会存在错误,需要Override
        float sum = 0F;
        if (absolute) {
            for (MathScalar term : this) {
                sum += FastMath.abs(term.getValue());
            }
        } else {
            for (MathScalar term : this) {
                sum += term.getValue();
            }
        }
        return sum;
    }

    /**
     * 获取方差
     * 
     * <pre>
     * 注意:
     * 获取方差(variance)的标准方法为value/size
     * 获取标准差(standardDeviation)的标准方法为Math.sqrt(value/size)
     * </pre>
     * 
     * @return
     */
    default Float2FloatKeyValue getVariance() {
        // TODO 此处对称矩阵可能会存在错误,需要Override
        float mean = Float.NaN;
        float variance = Float.NaN;
        int size = 0;
        Iterator<T> iterator = this.iterator();
        if (iterator.hasNext()) {
            MathScalar term = iterator.next();
            float value = term.getValue();
            mean = value;
            size = 1;
            variance = 0;
        }
        while (iterator.hasNext()) {
            MathScalar term = iterator.next();
            float value = term.getValue();
            float delta = (value - mean);
            size++;
            mean += delta / size;
            variance += delta * (value - mean);
        }
        // TODO 考虑处理跳过0的部分
        return new Float2FloatKeyValue(mean, variance);
    }

    /**
     * 获取方差
     * 
     * <pre>
     * 注意:
     * 获取方差(variance)的标准方法为value/size
     * 获取标准差(standardDeviation)的标准方法为Math.sqrt(value/size)
     * </pre>
     * 
     * @param mean
     * @return
     */
    default float getVariance(float mean) {
        // TODO 此处对称矩阵可能会存在错误,需要Override
        float variance = 0F;
        Iterator<T> iterator = this.iterator();
        while (iterator.hasNext()) {
            MathScalar term = iterator.next();
            float value = term.getValue();
            value = value - mean;
            variance += (value * value);
        }
        // TODO 考虑处理跳过0的部分
        return variance;
    }

}
