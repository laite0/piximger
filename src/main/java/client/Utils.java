package main.java.client;

import main.java.annotations.Internal;
import main.java.match.algebra.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {

    @Internal
    private static boolean is2019(String s, long threshold) {
        List<Character> numList = new StringBuilder(s)
                .reverse()
                .substring(4).chars()
                .mapToObj(i -> (char) i)
                .takeWhile(Character::isDigit).collect(Collectors.toList());
        char[] nums = new char[numList.size()];
        IntStream.range(0, numList.size()).forEach(i -> nums[nums.length - 1 - i] = numList.get(i));

        return toLongR(nums) >= threshold;
    }

    /**
     * Convert a int-indexed map to an array.
     * @apiNote time complexity of this function is essentially O(max index),
     * convert a large map would take extra-long time.
     * @param map sparse-array
     * @param <T> type of the element
     * @return an array
     */
    @SuppressWarnings("unchecked")
    public static <T> Pair<T[], /*solid*/Boolean> toArray(Map<Integer, T> map) {
        if (map.isEmpty()) return Pair.of((T[]) new Object[0], Boolean.TRUE);
        if (map.size() == 1 && map.containsKey(0)) return Pair.of((T[]) new Object[]{map.get(0)}, Boolean.TRUE);
        int size  = map.keySet().stream().max(Integer::compareTo).orElse(-1);
        T[] array = (T[]) new Object[size + 1];
        for (var e : map.entrySet()) {
            array[e.getKey()] = e.getValue();
        }
        return Pair.of(array, array.length == map.size());
    }

    @Internal
    private static long toLongR(char[] ch) {
        long in = 0L;
        //in = Long.valueOf(new StringBuffer().append(ch).reverse().toString());
        for (int i = ch.length - 1; i >= 0; i--) {
            long digit = Long.parseLong(String.valueOf(ch[i]));
            for (int j = 1; j < ch.length - i; j++) {
                digit *= 10L;
            }
            in += digit;
        }
        return in;
    }

    @Internal
    //used to retrieve tweet ID from a tweet address
    public static long recognizeID(String url, String author) {
        long id = -1;
        if (url.contains(author) & url.contains("status")) {
            String[] pair = url.split("status");
            try {
                id = Long.parseLong(pair[1]);
            } catch (NumberFormatException e) {
                id = takeIdAtExceptions(url, author);
            }
        } else if (!url.contains("status")) {
            try {
                id = Long.parseLong(url);
            } catch (NumberFormatException e) {
                id = takeIdAtExceptions(url, author);
            }
        }
        return id;
    }

    @Internal
    private static long takeIdAtExceptions(String url, String author) {
        url = url.replace(author, "");
        long id = -1;
        System.err.println("Error on " + url);
        List<Character> numList = url
                .chars().mapToObj(i -> (char) i)
                .dropWhile(c -> !Character.isDigit(c))
                .takeWhile(Character::isDigit).collect(Collectors.toList());
        char[]          nums    = new char[numList.size()];
        IntStream.range(0, numList.size()).forEach(i -> nums[i] = numList.get(i));
        id = toLongR(nums);
        return id;
    }

    /**
     * Throw a {@code Throwable} like a runtime exception
     * @param e the {@code Throwable}
     * @param <T> fake return type, aka. the {@code Nothing} type.
     * @return nothing
     */
    public static <T> T failInline(Throwable e) {
        return aET0(e);
    }


    /**
     * Throw an assertion error.
     * @param msg the message
     * @param <T> fake return type, aka. the {@code Nothing} type.
     * @return nothing
     */
    public static <T> T assertErr(String msg) {
        throw new AssertionError(msg);
    }

    /**
     * Print a {@code AssertionError} message in console and continue to execute.
     * @param msg the message
     * @param <T> the type of expression to evaluate after message prints.
     * @return expression
     */
    public static <T> T assertInfo(String msg, T next) {
        new AssertionError(msg).printStackTrace();
        return next;
    }

    @SuppressWarnings("unchecked")
    @Internal
    private static <T, E extends Throwable> T aET0(Throwable e) throws E {
        throw (E) e;
    }
}
