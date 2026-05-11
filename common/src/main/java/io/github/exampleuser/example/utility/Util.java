package io.github.exampleuser.example.utility;

import java.security.SecureRandom;

public final class Util {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a random string using alphanumeric characters
     *
     * @return a random string
     */
    public static String randomString() {
        return random.ints(generateRandomInt(1, 256), 0, CHARACTERS.length())
            .mapToObj(CHARACTERS::charAt)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    }

    /**
     * Generates a random string of specified length using alphanumeric characters
     *
     * @param length the desired length of the random string
     * @return a random string of the specified length
     */
    public static String randomString(int length) {
        return random.ints(length, 0, CHARACTERS.length())
            .mapToObj(CHARACTERS::charAt)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    }

    /**
     * Generates a random integer between lower and upper bounds (inclusive)
     *
     * @param lowerBound the minimum value (inclusive)
     * @param upperBound the maximum value (inclusive)
     * @return a random integer within the specified range
     */
    public static int generateRandomInt(int lowerBound, int upperBound) {
        return random.nextInt(lowerBound, upperBound + 1);
    }
}
