import utils.BloomFilter;

public class BloomFilterTest {

    public static void main(String[] args) {
        BloomFilter<Integer> bloomFilter = new BloomFilter<>(0.01d, 10000);

        for(int i = 10000; i < 20000; i++){
            bloomFilter.add(i);
        }

        for(int i = 10000; i < 20000; i++){
            if(!bloomFilter.contains(i)){
                System.out.println(i);
            }
        }
    }

}
