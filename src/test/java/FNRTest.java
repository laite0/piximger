package test.java;

import main.java.client.FileNameResolver;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class FNRTest {

    public static void main(String[] args) {
        var real = new FileNameResolver("1111 -_777_STR", false).resolve().validate(List.of(__ -> true, c -> {
            try {
                var i = Integer.parseInt(c);
                return true;
            } catch (NumberFormatException e) {
                System.out.println("FFFF!");
                return false;
            }
        })).collect(Collectors.toList());
        System.out.println(real);
    }

    private FNRTest() {
        throw new InternalError();
    }
}
