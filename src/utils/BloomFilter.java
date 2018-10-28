package utils;


import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Collection;

/**
 * 简单的BloomFilter
 *
 */
public class BloomFilter<T> implements Serializable {
    /**
     * 使用bitSet存储bit
     */
    private BitSet bitSet;

    private int bitSetSize;

    /**
     * 每个element占几位bit
     */
    private double bitsPerElement;

    /**
     * 预计最多会有多少元素会放入过滤器
     */
    private int expectNumbers;

    /**
     * 实际上的元素数量
     */
    private int actualNumbers;

    /**
     * hash函数的数量
     */
    private int k;

    private static final Charset charset = Charset.forName("UTF-8");

    /**
     * 使用md5哈希算法求hash
     */
    private static final String hashName = "md5";

    /**
     * 该类提供了信息摘要算法的功能
     */
    private static final MessageDigest digestFunction;

    static {
        MessageDigest tmp;
        try {
            tmp = MessageDigest.getInstance(hashName);
        } catch (NoSuchAlgorithmException e) {
            tmp = null;
        }

        digestFunction = tmp;
    }

    /**
     * 创建一个新的过滤器
     * @param bitsPerElement 每个元素占的bit数
     * @param expectNumbers 估计会有多少元素
     * @param k hash函数的个数
     */
    public BloomFilter(double bitsPerElement, int expectNumbers, int k){
        this.expectNumbers= expectNumbers;
        this.bitsPerElement = bitsPerElement;
        this.bitSetSize = (int)Math.ceil(bitsPerElement * expectNumbers);
        this.k = k;
        actualNumbers = 0;
        this.bitSet = new BitSet(bitSetSize);
    }

    /**
     * 给定bitset的大小和期望的元素数量建立bloom filter
     * @param bitSetSize bitSize的大小
     * @param expectNumbers 放入过滤器的最大元素数量
     */
    public BloomFilter(int bitSetSize, int expectNumbers){
        this((double) bitSetSize/expectNumbers, expectNumbers,
                (int)Math.round(bitSetSize/expectNumbers*Math.log(2)));
    }

    /**
     * 根据错误率和放入过滤器的最大元素数量建立Bloom Filter
     * @param falsePositiveProbability 错误率
     * @param expectNumbers 放入过滤器的最大的元素数量
     */
    public BloomFilter(double falsePositiveProbability, int expectNumbers){
        this(-Math.log(falsePositiveProbability) / Math.log(2) / Math.log(2), expectNumbers,
                (int)Math.ceil(-Math.log(falsePositiveProbability) / Math.log(2)));
    }

    /**
     * 根据String产生一个hash
     * @param val 输入
     * @param charset 输入的编码类型
     * @return val的hash
     */
    public static int createHash(String val, Charset charset){
        return createHash(val.getBytes(charset));
    }

    /**
     * 根据String产生一个hash
     * @param val 输入
     * @return
     */
    public static int createHash(String val){
        return createHash(val, charset);
    }

    /**
     * 根据输入产生一个hash
     * @param data 输入
     * @return
     */
    public static int createHash(byte []data){
        return createHashs(data, 1)[0];
    }

    /**
     * 根据输入的数据产生hashs个不同的hash值，
     * @param data 输入数据
     * @param hashs 产生的hash个数
     * @return
     */
    public static int[] createHashs(byte []data, int hashs){
        int []result = new int[hashs];

        int k = 0;
        //盐值
        byte salt = 0;
        while (k < hashs){
            byte []digest;
            //多线程同步
            synchronized (digestFunction){
                digestFunction.update(salt);
                //盐值加一
                salt ++;
                digest = digestFunction.digest(data);
            }

            //使用的是md5算法，digest是一个16byte的数组，每4byte组合成一个hash
            for (int i = 0; i < digest.length / 4 && k < hashs; i++){
                int h = 0;
                //求一个hash
                for (int j = i * 4; j < i * 4 + 4; j ++){
                    h <<= 8;
                    h |= digest[j] & 0xFF;
                }
                result[k] = h;
                k ++;
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(getClass() != obj.getClass())
            return false;
        BloomFilter bloomFilter = (BloomFilter)obj;
        if(this.expectNumbers != bloomFilter.expectNumbers || this.k != bloomFilter.k
        || this.bitSetSize != bloomFilter.bitSetSize){
            return false;
        }
        if(this.bitSet != bloomFilter.bitSet || (this.bitSet != null && !this.bitSet.equals(bloomFilter.bitSet))){
            return false;
        }
        return true;
    }

    /**
     * 返回现在的错误率
     * @return
    */
    public double getFalsePositiveProbability(){
        return getFalsePositiveProbability(actualNumbers);
    }

    /**
     * 返回给定一个添加了数量的错误率
     * @param actualNumbers 数量
     * @return
     */
    public double getFalsePositiveProbability(int actualNumbers){
        return Math.pow((1 - Math.exp(-k * actualNumbers / (double)bitSetSize)), k);
    }

    /**
     * 添加一个数据
     * @param element
     */
    public void add(T element){
        add(element.toString().getBytes(charset));
    }

    /**
     * 添加一个byte字节数据
     * @param bytes 数据
     */
    public void add(byte []bytes){
        int []hashs = createHashs(bytes, k);
        for(int hash : hashs){
            bitSet.set(Math.abs(hash % bitSetSize), true);
        }
        actualNumbers ++;
    }

    /**
     * 添加一个集合
     * @param c
     */
    public void addAll(Collection<? extends T> c){
        for(T e : c){
            add(e);
        }
    }

    /**
     * 判断bitset中是否存在
     * @param element
     * @return
     */
    public boolean contains(T element){
        return contains(element.toString().getBytes(charset));
    }

    /**
     * 判断byte是否存在bitset中
     * @param bytes
     * @return
     */
    public boolean contains(byte []bytes){
        int []hashs = createHashs(bytes, k);
        for(int hash : hashs){
            if(!bitSet.get(Math.abs(hash % bitSetSize))){
                return false;
            }
        }
        return true;
    }

    /**
     * 判断集合是否存在于Bloom Filter中
     * @param c 集合
     * @return
     */
    public boolean containsAll(Collection<? extends T> c){
        for(T e : c){
            if(!contains(e)){
                return false;
            }
        }
        return true;
    }

    /**
     * 获取对应的bit位
     * @param bit
     * @return
     */
    public boolean getBit(int bit){
        return bitSet.get(bit);
    }

    /**
     * 设置某一位的bit值
     * @param bit
     * @param value
     */
    public void setBit(int bit, boolean value){
        bitSet.set(bit, value);
    }

    /**
     * 返回平均每个元素所占的bit数
     * @return
     */
    public double getBitsPerElement(){
        return bitSetSize / actualNumbers;
    }

    /**
     * 返回bitSet的大小
     * @return
     */
    public int getBitSize(){
        return bitSetSize;
    }

}

